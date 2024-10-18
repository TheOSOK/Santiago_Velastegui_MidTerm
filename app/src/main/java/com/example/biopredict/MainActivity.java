package com.example.biopredict;

import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import kotlin.time.TestTimeSource;

public class MainActivity extends AppCompatActivity {

    EditText inputFieldTestTime;
    EditText inputFieldJitter;
    EditText inputFieldShimmer;
    EditText inputFieldRPDE;

    Button predictBtn;
    TextView resultTV;

    Interpreter interpreter;

    // Valores de normalización (deberías actualizar con los valores usados en Python)
    private final float[] minValues = {(float) -4.2625,(float) 0.00083,(float) 0.026,(float) 0.151};
    private final float[] maxValues = {(float)215.49,(float) 0.099,(float) 2.10,(float) 0.96};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Cargar el modelo
        try {
            interpreter = new Interpreter(loadModelFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Inicialización de los campos de entrada
        inputFieldTestTime = findViewById(R.id.editTextTestTime);
        inputFieldJitter = findViewById(R.id.editTextJitter);
        inputFieldShimmer = findViewById(R.id.editTextShimmer);
        inputFieldRPDE = findViewById(R.id.editTextRPDE);

        predictBtn = findViewById(R.id.button);
        resultTV = findViewById(R.id.textView);

        predictBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Verificar que los campos no estén vacíos
                if (areInputsValid()) {
                    try {
                        // Obtener los valores de entrada y normalizarlos
                        float TestTimeValue = normalize(Float.parseFloat(inputFieldTestTime.getText().toString()), (float) minValues[0], maxValues[0]);
                        float JitterValue = normalize(Float.parseFloat(inputFieldJitter.getText().toString()), minValues[1], maxValues[1]);
                        float ShimmerValue = normalize(Float.parseFloat(inputFieldShimmer.getText().toString()), minValues[2], maxValues[2]);
                        float RPDEValue = normalize(Float.parseFloat(inputFieldRPDE.getText().toString()), minValues[3], maxValues[3]);

                        // Crear la matriz de entrada
                        float[][] inputs = new float[1][4];
                        inputs[0][0] = TestTimeValue;
                        inputs[0][1] = JitterValue;
                        inputs[0][2] = ShimmerValue;
                        inputs[0][3] = RPDEValue;

                        // Realizar la inferencia
                        float result = doInference(inputs);

                        // Desnormalizar el resultado
                        float desnormalizedResult = desnormalize(result, 7f, 54.92f); // Cambia estos valores a los min y max de y

                        // Mostrar resultado
                        resultTV.setText("Result: " + desnormalizedResult);

                    } catch (NumberFormatException e) {
                        resultTV.setText("Invalid input. Please enter numeric values.");
                    } catch (Exception e) {
                        resultTV.setText("An error occurred.");
                    }
                } else {
                    resultTV.setText("Please enter valid inputs.");
                }
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private boolean areInputsValid() {
        // Verifica que los campos no estén vacíos y que sean números válidos
        return !inputFieldTestTime.getText().toString().isEmpty() &&
                !inputFieldJitter.getText().toString().isEmpty() &&
                !inputFieldShimmer.getText().toString().isEmpty() &&
                !inputFieldRPDE.getText().toString().isEmpty();
    }

    private float normalize(float value, float min, float max) {
        return (value - min) / (max - min);
    }

    private float desnormalize(float value, float min, float max) {
        return value * (max - min) + min;
    }

    public float doInference(float[][] input) {
        float[][] output = new float[1][1];
        interpreter.run(input, output);
        return output[0][0];
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor assetFileDescriptor = this.getAssets().openFd("MIDTERM_linear.tflite");
        FileInputStream fileInputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
        FileChannel fileChannel = fileInputStream.getChannel();
        long startOffset = assetFileDescriptor.getStartOffset();
        long length = assetFileDescriptor.getLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, length);
    }
}

