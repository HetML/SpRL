package edu.tulane.cs.hetml.nlp.sprl

import java.io.File
import java.math.BigInteger

import edu.illinois.cs.cogcomp.core.utilities.XmlModel
import edu.illinois.cs.cogcomp.saul.util.Logging
import edu.tulane.cs.hetml.nlp.sprl.Helpers.FeatureSets
import edu.tulane.cs.hetml.nlp.sprl.MultiModalSpRLClassifiers._
import edu.tulane.cs.hetml.nlp.BaseTypes._
import edu.illinois.cs.cogcomp.saulexamples.nlp.SpatialRoleLabeling.SpRL2013.{LANDMARK, RELATION, SPATIALINDICATOR, TRAJECTOR}
import edu.illinois.cs.cogcomp.saulexamples.nlp.SpatialRoleLabeling._
import edu.tulane.cs.hetml.nlp.sprl.Helpers.FeatureSets
import edu.tulane.cs.hetml.nlp.sprl.MultiModalSpRLClassifiers._

import scala.collection.JavaConversions._

/** Created by taher on 2017-02-24.
  */
object SpRLApp extends App with Logging {

  import MultiModalSpRLDataModel._

  override def main(args: Array[String]): Unit = {

    MultiModalSpRLClassifiers.featureSet = FeatureSets.WordEmbedding
    MultiModalSpRLDataModel.useVectorAverages = false

    val classifiers = List(
      IndicatorRoleClassifier,
      TrajectorPairClassifier,
      LandmarkPairClassifier
    )

    classifiers.foreach(x => {
      x.modelDir = s"models/mSpRL/$featureSet/"
      x.load()
    })

    var sCount = 0
    var trCount = 0
    var spCount = 0
    var lmCount = 0
    var rCount = 0

    if (args.length % 2 == 1) {
      println("SpRLApp -i inputPath -o outputFile")
      return
    }
    var input = "data/mSprl/input/"
    var output = "output.xml"
    args.sliding(2, 2).toList.collect {
      case Array("-i", x) => input = x
      case Array("-o", x) => output = x
    }
    val inputDir = new File(input)
    if (!inputDir.exists() || inputDir.listFiles().length == 0) {
      println("input directory doesn't exists or is empty.")
      return
    }
    val documentList = inputDir.listFiles.filter(f=> f.getName.endsWith(".txt")).map(file => {
      val text = scala.io.Source.fromFile(file).mkString
      new Document(file.getName, -1, -1, text)
    }).toList

    MultiModalPopulateData.populateDataFromPlainTextDocuments(documentList, x=> IndicatorRoleClassifier(x) == "Indicator")

    val relationList = TripletClassifierUtils.predict(
      x => TrajectorPairClassifier(x) == "TR-SP",
      x => IndicatorRoleClassifier(x) == "Indicator",
      x => LandmarkPairClassifier(x) == "LM-SP"
    )
    relationList.foreach(r => r.setParent(r.getArgument(1).asInstanceOf[Phrase].getSentence))

    val spRL2017Document = new SpRL2017.SpRL2017Document
    spRL2017Document.setScenes(documentList.map(getScene))
    XmlModel.write(spRL2017Document, output)

    ////////////////////////////////////////////////////////////////////////////////////////////////
    def getScene(d: Document) = {
      val s = new SpRL2017.Scene()
      s.setDocNo(d.getId)
      val sentenceList = (documents(d) ~> documentToSentence).map(getSentence).toList.sortBy(x => x.getStart)
      s.setSentences(sentenceList)
      s
    }

    def getSentence(sentence: Sentence) = {
      val s = new SpRL2017.Sentence()
      sCount = sCount + 1
      s.setId(s"s$sCount")
      sentence.setId(s"S$sCount")
      s.setStart(sentence.getStart)
      s.setEnd(sentence.getEnd)
      s.setText(sentence.getText)
      setAnnotations(sentence, s)
      s
    }

    def setAnnotations(sentence: Sentence, s: SpRL2017.Sentence) = {
      val rels = relationList.filter(_.getParent.getId == sentence.getId).sortBy(x => x.getArgument(1).getStart)

      val trajectors = rels.map(_.getArgument(0)).distinct.sortBy(_.getStart)
        .map(x => x.getId -> setInfo(new TRAJECTOR, x)).toMap

      val indicators = rels.map(_.getArgument(1)).distinct.sortBy(_.getStart)
        .map(x => x.getId -> setInfo(new SPATIALINDICATOR, x)).toMap

      val landmarks = rels.map(_.getArgument(2)).distinct.sortBy(_.getStart)
        .map(x => x.getId -> setInfo(new LANDMARK, x)).toMap

      s.setTrajectors(trajectors.values.toList.sortBy(_.getStart))
      s.setSpatialindicators(indicators.values.toList.sortBy(_.getStart))
      s.setLandmarks(landmarks.values.toList.sortBy(_.getStart))

      val relAnnotations = rels.map(r => {
        val rel = new RELATION()
        rCount = rCount + 1
        rel.setId(s"SR$rCount")
        rel.setTrajectorId(trajectors(r.getArgumentId(0)).getId)
        rel.setSpatialIndicatorId(indicators(r.getArgumentId(1)).getId)
        rel.setLandmarkId(landmarks(r.getArgumentId(2)).getId)
        rel
      })
      s.setRelations(relAnnotations)
    }

    def setInfo[T <: SpRLAnnotation](e: T, x: NlpBaseElement): T = {

      (e match {
        case y: TRAJECTOR =>
          trCount = trCount + 1
          y.setId(s"T$trCount")
          y.setStart(x.getStart)
          y.setEnd(x.getEnd)
          y.setText(x.getText)
          y
        case y: SPATIALINDICATOR =>
          spCount = spCount + 1
          y.setId(s"S$spCount")
          y.setStart(x.getStart)
          y.setEnd(x.getEnd)
          y.setText(x.getText)

          y
        case y: LANDMARK =>
          lmCount = lmCount + 1
          y.setId(s"L$lmCount")
          y.setStart(x.getStart)
          y.setEnd(x.getEnd)
          y.setText(x.getText)
          y
      }).asInstanceOf[T]
    }
  }
}
