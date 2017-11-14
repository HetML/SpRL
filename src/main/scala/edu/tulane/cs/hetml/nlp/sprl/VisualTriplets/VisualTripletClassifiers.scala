package edu.tulane.cs.hetml.nlp.sprl.VisualTriplets

import edu.illinois.cs.cogcomp.lbjava.learn.{SparseNetworkLearner, SupportVectorMachine}
import edu.illinois.cs.cogcomp.saul.classifier.Learnable
import edu.tulane.cs.hetml.nlp.sprl.VisualTriplets.VisualTripletsDataModel._

object VisualTripletClassifiers {

  object VisualTripletClassifier extends Learnable(visualTriplets) {
    def label = visualTripletLabel

    override lazy val classifier = new SupportVectorMachine()

    override def feature = List(visualTripletTrajector, visualTripletlandmark, visualTripletTrajectorW2V,
      visualTripletlandmarkW2V, visualTripletTrVector, visualTripletTrajectorAreaWRTLanmark, visualTripletTrajectorAspectRatio,
      visualTripletLandmarkAspectRatio, visualTripletTrajectorAreaWRTBbox, visualTripletLandmarkAreaWRTBbox, visualTripletIOU,
      visualTripletEuclideanDistance, visualTripletTrajectorAreaWRTImage, visualTripletLandmarkAreaWRTImage)
  }
}
