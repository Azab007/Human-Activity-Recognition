package io.github.introml.activityrecognition;

import android.content.Context;
import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;


public class TensorFlowClassifier {
    static {
        System.loadLibrary("tensorflow_inference");
    }

    private TensorFlowInferenceInterface inferenceInterface1, inferenceInterface2;
    private static final String MODEL_FILE1 = "file:///android_asset/Acc_model.pb";
    private static final String MODEL_FILE2 = "file:///android_asset/Gyro_model.pb";

    private static final String INPUT_NODE = "conv1d_1_input";
    private static final String[] OUTPUT_NODES1 = {"dense_3/Softmax"};
    private static final String OUTPUT_NODE1 = "dense_3/Softmax";

    private static final String[] OUTPUT_NODES2 = {"dense_2/Softmax"};
    private static final String OUTPUT_NODE2 = "dense_2/Softmax";

    private static final long[] INPUT_SIZE = {1, 100, 3};
    private static final int OUTPUT_SIZE = 4;

    public TensorFlowClassifier(final Context context) {
        inferenceInterface1 = new TensorFlowInferenceInterface(context.getAssets(), MODEL_FILE1);
        inferenceInterface2 = new TensorFlowInferenceInterface(context.getAssets(), MODEL_FILE2);
    }

    public float[] predictProbabilities(float[] data, boolean isAcc) {
        Log.i("000000000", Boolean.toString(isAcc));
        float[] result = new float[OUTPUT_SIZE];
        if(isAcc){
            inferenceInterface1.feed(INPUT_NODE, data, INPUT_SIZE);
            inferenceInterface1.run(OUTPUT_NODES1);
            inferenceInterface1.fetch(OUTPUT_NODE1, result);
        }
        else{
            inferenceInterface2.feed(INPUT_NODE, data, INPUT_SIZE);
            inferenceInterface2.run(OUTPUT_NODES2);
            inferenceInterface2.fetch(OUTPUT_NODE2, result);
        }

        return result;
    }
}
