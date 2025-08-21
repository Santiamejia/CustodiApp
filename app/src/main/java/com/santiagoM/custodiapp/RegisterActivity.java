package com.santiagoM.custodiapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportesActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // UI Elements
    private ImageView ivBackArrow, ivAddContact;
    private CardView cardContactsPanel;
    private EditText etContactInput;
    private Button btnCancelAdd, btnAddContact, btnMyLocation, btnRefreshLocations;
    private RecyclerView rvTrackedContacts;
    private ProgressBar progressLoading;

    // Google Maps
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Marker myLocationMarker;
    private Map<String, Marker> contactMarkers = new HashMap<>();

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;

    // Data
    private List<TrackedContact> trackedContacts = new ArrayList<>();
    private TrackedContactsAdapter contactsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes);

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
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        // Inicializar servicios de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupRecyclerView();
        setupListeners();
        setupMap();
        requestLocationPermission();
    }

    private void initViews() {
        ivBackArrow = findViewById(R.id.iv_back_arrow);
        ivAddContact = findViewById(R.id.iv_add_contact);
        cardContactsPanel = findViewById(R.id.card_contacts_panel);
        etContactInput = findViewById(R.id.et_contact_input);
        btnCancelAdd = findViewById(R.id.btn_cancel_add);
        btnAddContact = findViewById(R.id.btn_add_contact);
        btnMyLocation = findViewById(R.id.btn_my_location);
        btnRefreshLocations = findViewById(R.id.btn_refresh_locations);
        rvTrackedContacts = findViewById(R.id.rv_tracked_contacts);
        progressLoading = findViewById(R.id.progress_loading);
    }

    private void setupRecyclerView() {
        contactsAdapter = new TrackedContactsAdapter(trackedContacts, this::removeTrackedContact);
        rvTrackedContacts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvTrackedContacts.setAdapter(contactsAdapter);
    }

    private void setupListeners() {
        // Volver atrás
        ivBackArrow.setOnClickListener(v -> finish());

        // Mostrar/ocultar panel de agregar contacto
        ivAddContact.setOnClickListener(v -> toggleContactPanel());
        btnCancelAdd.setOnClickListener(v -> hideContactPanel());

        // Agregar contacto
        btnAddContact.setOnClickListener(v -> addNewContact());

        // Controles del mapa
        btnMyLocation.setOnClickListener(v -> centerMapOnMyLocation());
        btnRefreshLocations.setOnClickListener(v -> refreshAllLocations());
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Configurar el mapa
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false); // Usamos nuestro botón personalizado

        // Centrar en Colombia por defecto
        LatLng colombia = new LatLng(4.7110, -74.0721);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(colombia, 6));

        // Cargar contactos y ubicaciones
        loadTrackedContacts();
        getCurrentLocation();
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Permiso de ubicación necesario para el funcionamiento", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        updateMyLocationOnMap(location);
                        saveMyLocationToFirebase(location);
                    }
                });
    }

    private void updateMyLocationOnMap(Location location) {
        if (mMap == null) return;

        LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        // Remover marcador anterior si existe
        if (myLocationMarker != null) {
            myLocationMarker.remove();
        }

        // Agregar nuevo marcador para mi ubicación
        myLocationMarker = mMap.addMarker(new MarkerOptions()
                .position(myLatLng)
                .title("Mi ubicación")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
    }

    private void saveMyLocationToFirebase(Location location) {
        if (currentUserId.isEmpty()) return;

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("location", new GeoPoint(location.getLatitude(), location.getLongitude()));
        locationData.put("lastUpdated", System.currentTimeMillis());
        locationData.put("isActive", true);

        db.collection("userLocations").document(currentUserId)
                .set(locationData)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al actualizar ubicación", Toast.LENGTH_SHORT).show());
    }

    private void toggleContactPanel() {
        if (cardContactsPanel.getVisibility() == android.view.View.GONE) {
            cardContactsPanel.setVisibility(android.view.View.VISIBLE);
            etContactInput.requestFocus();
        } else {
            hideContactPanel();
        }
    }

    private void hideContactPanel() {
        cardContactsPanel.setVisibility(android.view.View.GONE);
        etContactInput.setText("");
    }

    private void addNewContact() {
        String input = etContactInput.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "Ingresa un email o número de teléfono", Toast.LENGTH_SHORT).show();
            return;
        }

        progressLoading.setVisibility(android.view.View.VISIBLE);

        // Buscar usuario por email o teléfono
        db.collection("users")
                .whereEqualTo("email", input)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Usuario encontrado
                        String userId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        String userName = queryDocumentSnapshots.getDocuments().get(0).getString("firstName");
                        addContactToTracking(userId, userName, input);
                    } else {
                        // Buscar por teléfono (si implementas campo teléfono)
                        searchByPhone(input);
                    }
                    progressLoading.setVisibility(android.view.View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressLoading.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Error al buscar usuario", Toast.LENGTH_SHORT).show();
                });
    }

    private void searchByPhone(String phone) {
        // TODO: Implementar búsqueda por teléfono si agregas ese campo
        Toast.makeText(this, "Usuario no encontrado con ese email", Toast.LENGTH_SHORT).show();
    }

    private void addContactToTracking(String userId, String userName, String identifier) {
        TrackedContact contact = new TrackedContact(userId, userName, identifier);

        // Agregar a la lista local
        trackedContacts.add(contact);
        contactsAdapter.notifyDataSetChanged();

        // Guardar en Firebase (lista de contactos que estoy rastreando)
        Map<String, Object> trackingData = new HashMap<>();
        trackingData.put("userId", userId);
        trackingData.put("userName", userName);
        trackingData.put("identifier", identifier);
        trackingData.put("addedAt", System.currentTimeMillis());

        db.collection("userTracking").document(currentUserId)
                .collection("trackedContacts").document(userId)
                .set(trackingData);

        // Cargar ubicación del contacto
        loadContactLocation(contact);

        hideContactPanel();
        Toast.makeText(this, "Contacto agregado: " + userName, Toast.LENGTH_SHORT).show();
    }

    private void loadTrackedContacts() {
        db.collection("userTracking").document(currentUserId)
                .collection("trackedContacts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    trackedContacts.clear();
                    for (var doc : queryDocumentSnapshots.getDocuments()) {
                        TrackedContact contact = new TrackedContact(
                                doc.getString("userId"),
                                doc.getString("userName"),
                                doc.getString("identifier")
                        );
                        trackedContacts.add(contact);
                        loadContactLocation(contact);
                    }
                    contactsAdapter.notifyDataSetChanged();
                });
    }

    private void loadContactLocation(TrackedContact contact) {
        db.collection("userLocations").document(contact.getUserId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        GeoPoint geoPoint = documentSnapshot.getGeoPoint("location");
                        if (geoPoint != null) {
                            updateContactMarkerOnMap(contact, geoPoint);
                        }
                    }
                });
    }

    private void updateContactMarkerOnMap(TrackedContact contact, GeoPoint geoPoint) {
        if (mMap == null) return;

        LatLng position = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());

        // Remover marcador anterior si existe
        Marker existingMarker = contactMarkers.get(contact.getUserId());
        if (existingMarker != null) {
            existingMarker.remove();
        }

        // Agregar nuevo marcador
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(position)
                .title(contact.getUserName())
                .snippet("Última actualización: ahora")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        contactMarkers.put(contact.getUserId(), marker);
    }

    private void removeTrackedContact(TrackedContact contact) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar contacto")
                .setMessage("¿Dejar de rastrear a " + contact.getUserName() + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    // Remover de Firebase
                    db.collection("userTracking").document(currentUserId)
                            .collection("trackedContacts").document(contact.getUserId())
                            .delete();

                    // Remover de la lista local
                    trackedContacts.remove(contact);
                    contactsAdapter.notifyDataSetChanged();

                    // Remover marcador del mapa
                    Marker marker = contactMarkers.get(contact.getUserId());
                    if (marker != null) {
                        marker.remove();
                        contactMarkers.remove(contact.getUserId());
                    }

                    Toast.makeText(this, "Contacto eliminado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void centerMapOnMyLocation() {
        if (myLocationMarker != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    myLocationMarker.getPosition(), 15));
        } else {
            getCurrentLocation();
        }
    }

    private void refreshAllLocations() {
        getCurrentLocation();
        for (TrackedContact contact : trackedContacts) {
            loadContactLocation(contact);
        }
        Toast.makeText(this, "Actualizando ubicaciones...", Toast.LENGTH_SHORT).show();
    }

    // Clase interna para representar un contacto rastreado
    public static class TrackedContact {
        private String userId;
        private String userName;
        private String identifier;

        public TrackedContact(String userId, String userName, String identifier) {
            this.userId = userId;
            this.userName = userName;
            this.identifier = identifier;
        }

        public String getUserId() { return userId; }
        public String getUserName() { return userName; }
        public String getIdentifier() { return identifier; }
    }
}