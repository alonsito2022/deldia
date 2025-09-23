package com.sys4soft.deldia.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.sys4soft.deldia.R

class MapLocationDialog : DialogFragment(), OnMapReadyCallback {

    interface OnLocationSelectedListener {
        fun onLocationSelected(latitude: Double, longitude: Double)
    }

    private var listener: OnLocationSelectedListener? = null
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    
    private lateinit var textViewLatitude: TextView
    private lateinit var textViewLongitude: TextView
    private lateinit var btnConfirmLocation: Button
    private lateinit var btnCancelLocation: Button
    private lateinit var btnCloseDialog: ImageButton
    
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0

    companion object {
        private const val ARG_INITIAL_LATITUDE = "initial_latitude"
        private const val ARG_INITIAL_LONGITUDE = "initial_longitude"
        
        fun newInstance(listener: OnLocationSelectedListener, initialLatitude: Double = 0.0, initialLongitude: Double = 0.0): MapLocationDialog {
            val dialog = MapLocationDialog()
            dialog.listener = listener
            
            // Pasar coordenadas iniciales como argumentos
            val args = Bundle()
            args.putDouble(ARG_INITIAL_LATITUDE, initialLatitude)
            args.putDouble(ARG_INITIAL_LONGITUDE, initialLongitude)
            dialog.arguments = args
            
            return dialog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_map_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupClickListeners()
        
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragmentDialog) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    private fun initializeViews(view: View) {
        textViewLatitude = view.findViewById(R.id.textViewLatitudeDialog)
        textViewLongitude = view.findViewById(R.id.textViewLongitudeDialog)
        btnConfirmLocation = view.findViewById(R.id.btnConfirmLocation)
        btnCancelLocation = view.findViewById(R.id.btnCancelLocation)
        btnCloseDialog = view.findViewById(R.id.btnCloseDialog)
    }

    private fun setupClickListeners() {
        btnCloseDialog.setOnClickListener {
            dismiss()
        }
        
        btnCancelLocation.setOnClickListener {
            dismiss()
        }
        
        btnConfirmLocation.setOnClickListener {
            listener?.onLocationSelected(currentLatitude, currentLongitude)
            dismiss()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        mMap.uiSettings.isZoomControlsEnabled = true
        
        // Configurar listeners para actualizar coordenadas
        mMap.setOnCameraMoveListener {
            updateCoordinatesFromMapCenter()
        }
        
        mMap.setOnCameraIdleListener {
            updateCoordinatesFromMapCenter()
        }

        fetchCurrentLocation()
    }
    
    private fun updateCoordinatesFromMapCenter() {
        val center = mMap.cameraPosition.target
        currentLatitude = center.latitude
        currentLongitude = center.longitude
        
        textViewLatitude.text = String.format("%.6f", currentLatitude)
        textViewLongitude.text = String.format("%.6f", currentLongitude)
    }

    private fun fetchCurrentLocation() {
        // Obtener coordenadas iniciales de los argumentos
        val initialLatitude = arguments?.getDouble(ARG_INITIAL_LATITUDE, 0.0) ?: 0.0
        val initialLongitude = arguments?.getDouble(ARG_INITIAL_LONGITUDE, 0.0) ?: 0.0
        
        // Si hay coordenadas iniciales válidas, usarlas
        if (initialLatitude != 0.0 && initialLongitude != 0.0) {
            val initialLocation = LatLng(initialLatitude, initialLongitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 15.0f))
            updateCoordinatesFromMapCenter()
            return
        }
        
        // Si no hay coordenadas iniciales, intentar obtener la ubicación actual
        if (androidx.core.app.ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED && 
            androidx.core.app.ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            // Si no hay permisos, usar una ubicación por defecto (Arequipa, Perú)
            val defaultLocation = LatLng(-16.4090, -71.5375)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15.0f))
            updateCoordinatesFromMapCenter()
            return
        }
        
        val task = fusedLocationProviderClient.lastLocation
        task.addOnSuccessListener { location ->
            if (location != null) {
                val currentLocation = LatLng(location.latitude, location.longitude)
                mMap.isMyLocationEnabled = true
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15.0f))
                updateCoordinatesFromMapCenter()
            } else {
                // Si no se puede obtener la ubicación, usar ubicación por defecto
                val defaultLocation = LatLng(-16.4090, -71.5375)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15.0f))
                updateCoordinatesFromMapCenter()
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return dialog
    }
}
