package com.santiagoM.custodiapp;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private ImageView ivPasswordToggle;
    private Button btnSignIn;
    private TextView tvDivider, tvSignUp;
    private boolean isPasswordVisible = false;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Cambiar color de la barra de estado a gris
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        }

        // Ocultar action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        ivPasswordToggle = findViewById(R.id.iv_password_toggle);
        btnSignIn = findViewById(R.id.btn_sign_in);
        tvDivider = findViewById(R.id.tv_divider);
        tvSignUp = findViewById(R.id.tv_sign_up); // El TextView "Registrarse"
    }

    private void setupListeners() {
        // Toggle contraseña
        ivPasswordToggle.setOnClickListener(v -> togglePasswordVisibility());

        // Botón Sign In - AQUÍ ESTÁ LA CONEXIÓN A HOME
        btnSignIn.setOnClickListener(v -> handleSignIn());

        // Link "Accede desde tu cuenta" (si quieres mantenerlo)
        tvDivider.setOnClickListener(v -> {
            Toast.makeText(this, "Opciones de acceso", Toast.LENGTH_SHORT).show();
        });

        // CONEXIÓN A REGISTRO - Link "Registrarse"
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            ivPasswordToggle.setImageResource(android.R.drawable.ic_menu_view);
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            ivPasswordToggle.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        }
        etPassword.setSelection(etPassword.getText().length());
        isPasswordVisible = !isPasswordVisible;
    }

    private void handleSignIn() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar loading
        btnSignIn.setEnabled(false);
        btnSignIn.setText("Iniciando sesión...");

        // Autenticar con Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // ✅ LOGIN EXITOSO - IR A HOME
                        Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(this, activity_home.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                    } else {
                        // ❌ ERROR EN LOGIN
                        btnSignIn.setEnabled(true);
                        btnSignIn.setText("Sign In");
                        String error = task.getException() != null ? task.getException().getMessage() : "Error desconocido";
                        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Verificar si el usuario ya está autenticado
        if (mAuth.getCurrentUser() != null) {
            // Usuario ya está logueado, ir directamente a Home
            Intent intent = new Intent(this, activity_home.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}