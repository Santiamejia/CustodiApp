package com.santiagoM.custodiapp;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class activity_home extends AppCompatActivity {

    private TextView tvUserName;
    private LinearLayout optionPerfil, optionReportes, optionFavoritos, optionAcercaDe;
    private Button btnLogout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Cambiar color de la barra de estado a verde
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.green_primary));
        }

        // Ocultar action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupListeners();
        loadUserData();
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tv_user_name);
        optionPerfil = findViewById(R.id.option_perfil);
        optionReportes = findViewById(R.id.option_reportes);
        optionFavoritos = findViewById(R.id.option_favoritos);
        optionAcercaDe = findViewById(R.id.option_acerca_de);
        btnLogout = findViewById(R.id.btn_logout);
    }

    private void setupListeners() {
        // Opciones del menú principal
        optionPerfil.setOnClickListener(v -> openPerfil());
        optionReportes.setOnClickListener(v -> openReportes());
        optionFavoritos.setOnClickListener(v -> openFavoritos());
        optionAcercaDe.setOnClickListener(v -> openAcercaDe());

        // Botón cerrar sesión
        btnLogout.setOnClickListener(v -> logout());
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Cargar datos del usuario desde Firestore
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String firstName = documentSnapshot.getString("firstName");
                            String lastName = documentSnapshot.getString("lastName");

                            if (firstName != null) {
                                String fullName = firstName + " " + (lastName != null ? lastName : "");
                                tvUserName.setText(fullName);
                            }
                        } else {
                            tvUserName.setText("Usuario");
                        }
                    })
                    .addOnFailureListener(e -> {
                        tvUserName.setText("Usuario");
                    });
        } else {
            // Usuario no autenticado, redirigir al login
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void openPerfil() {
        Toast.makeText(this, "👤 Abriendo Perfil", Toast.LENGTH_SHORT).show();
        // TODO: Intent intent = new Intent(this, PerfilActivity.class);
        // TODO: startActivity(intent);
    }

    private void openReportes() {
        Toast.makeText(this, "📊 Abriendo Reportes", Toast.LENGTH_SHORT).show();
        // TODO: Intent intent = new Intent(this, ReportesActivity.class);
        // TODO: startActivity(intent);
    }

    private void openFavoritos() {
        Toast.makeText(this, "⭐ Abriendo Favoritos", Toast.LENGTH_SHORT).show();
        // TODO: Intent intent = new Intent(this, FavoritosActivity.class);
        // TODO: startActivity(intent);
    }

    private void openAcercaDe() {
        Toast.makeText(this, "ℹ️ Abriendo Acerca de", Toast.LENGTH_SHORT).show();
        // TODO: Intent intent = new Intent(this, AcercaDeActivity.class);
        // TODO: startActivity(intent);
    }

    private void logout() {
        mAuth.signOut();
        Toast.makeText(this, "👋 Sesión cerrada", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity(); // Cierra toda la app
    }
}