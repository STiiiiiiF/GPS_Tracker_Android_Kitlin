package com.zakharov.gpstracker.fragments

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.zakharov.gpstracker.MainApp
import com.zakharov.gpstracker.MainViewModel
import com.zakharov.gpstracker.R
import com.zakharov.gpstracker.databinding.FragmentMainBinding
import com.zakharov.gpstracker.db.TrackItem
import com.zakharov.gpstracker.location.LocationModel
import com.zakharov.gpstracker.location.LocationService
import com.zakharov.gpstracker.utils.DialogManager
import com.zakharov.gpstracker.utils.TimeUtils
import com.zakharov.gpstracker.utils.checkPermission
import org.osmdroid.config.Configuration
import org.osmdroid.library.BuildConfig
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.Serializable
import java.util.*
import java.lang.StringBuilder


class MainFragment : Fragment() {

    private var locationModel: LocationModel? = null
    private var polyLine: Polyline? = null
    private var firstStart = true
    private val model: MainViewModel by activityViewModels {
        MainViewModel.ViewModelFactory((requireContext().applicationContext as MainApp).database)
    }
    private var isServiceRunning = false
    private var timer: Timer? = null
    private var startTime = 0L
    private lateinit var mLocOverlay: MyLocationNewOverlay
    private lateinit var pLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var binding: FragmentMainBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        settingOsm()
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerPermissions()
        setOnClicks()
        checkServiceState()
        updateTime()
        registerLocReceiver()
        locationUpdates()
    }

    private fun setOnClicks() = with(binding) {
        val listener = onClicks()
        fStartStop.setOnClickListener(listener)
        fCenter.setOnClickListener(listener)

    }

    private fun onClicks(): OnClickListener {
        return OnClickListener {
            when (it.id) {
                R.id.fStartStop -> startStopService()
                R.id.fCenter -> centerLocation()
            }
        }
    }

    private fun centerLocation() {
        binding.map.controller.animateTo(mLocOverlay.myLocation)
        mLocOverlay.enableFollowLocation()
    }


    private fun locationUpdates() = with(binding) {
        model.locationUpdates.observe(viewLifecycleOwner) {
            val distance: String = if (it.distance >= 1000) {
                "Distance: ${String.format("%.1f", it.distance / 1000)} km"
            } else "Distance: ${String.format("%.1f", it.distance)} m"
            val speed = "Speed: ${String.format("%.1f", 3.6f * it.speed)} km/h"
            val averageSpeed = "Average speed: ${getAverageSpeed(it.distance)} km/h"
            tvDistance.text = distance
            tvSpeed.text = speed
            tvAverageSpeed.text = averageSpeed
            locationModel = it
            updatePolyLine(it.geoPointsList)
        }
    }

    private fun updateTime() {
        model.timeData.observe(viewLifecycleOwner) {
            binding.tvTime.text = it
        }
    }


    private fun startTimer() {
        timer?.cancel()
        timer = Timer()
        startTime = LocationService.startTime
        timer?.schedule(object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    model.timeData.value = getCurrentTime()
                }
            }
        }, 1, 1)
    }

    private fun getAverageSpeed(distance: Float): String {
        return String.format(
            "%.1f",
            3.6f * (distance / ((System.currentTimeMillis() - startTime) / 1000f))
        )
    }

    private fun getCurrentTime(): String {
        return "Time: ${TimeUtils.getTime(System.currentTimeMillis() - startTime)}"
    }

    private fun geoPointsToString(list: List<GeoPoint>): String {
        val sb = StringBuilder()
        list.forEach {
            sb.append("${it.latitude}, ${it.longitude}/")
        }
        return sb.toString()
    }

    private fun startStopService() {
        if (!isServiceRunning) {
            startLocService()
        } else {
            activity?.stopService(Intent(activity, LocationService::class.java))
            binding.fStartStop.setImageResource(R.drawable.ic_play)
            timer?.cancel()
            val track = getTrackItem()
            DialogManager.showSaveDialog(
                requireContext(),
                track,
                object : DialogManager.Listener {
                    override fun onClick() {
                        Toast.makeText(activity, "Saved!", Toast.LENGTH_SHORT).show()
                        model.insertTrack(track)
                    }
                })
        }
        isServiceRunning = !isServiceRunning
    }

    private fun getTrackItem(): TrackItem {
        return TrackItem(
            null,
            getCurrentTime(),
            TimeUtils.getDate(),
            String.format("%.1f", locationModel?.distance?.div(1000) ?: 0),
            getAverageSpeed(locationModel?.distance ?: 0.0f),
            geoPointsToString(locationModel?.geoPointsList ?: listOf())
        )
    }

    private fun checkServiceState() {
        isServiceRunning = LocationService.isRunning
        if (isServiceRunning) {
            binding.fStartStop.setImageResource(R.drawable.ic_stop)
            startTimer()
        }
    }

    private fun startLocService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity?.startForegroundService(Intent(activity, LocationService::class.java))
        } else {
            activity?.startService(Intent(activity, LocationService::class.java))
        }
        binding.fStartStop.setImageResource(R.drawable.ic_stop)
        LocationService.startTime = System.currentTimeMillis()
        startTimer()
    }

    override fun onResume() {
        super.onResume()
        checkLocPermission()
        firstStart = true
    }


    private fun settingOsm() {
        Configuration.getInstance().load(
            activity as AppCompatActivity,
            activity?.getSharedPreferences("osm_pref", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
    }

    private fun initOSM() = with(binding) {
        polyLine = Polyline()
        polyLine?.outlinePaint?.color = Color.parseColor(
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString("color_key", "#FF000000")
        )
        map.controller.setZoom(20.0)
        val mLocProvider = GpsMyLocationProvider(activity)
        mLocOverlay = MyLocationNewOverlay(mLocProvider, map)
        mLocOverlay.enableMyLocation()
        mLocOverlay.enableFollowLocation()
        mLocOverlay.runOnFirstFix {
            map.overlays.clear()
            map.overlays.add(polyLine)
            map.overlays.add(mLocOverlay)

        }
    }

    private fun registerPermissions() {
        pLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                if (it[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                    initOSM()
                    checkLocationEnabled()
                } else {
                    Toast.makeText(
                        activity,
                        "You didn't give permission location",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun checkLocPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkPermissionAfter10()
        } else {
            checkPermissionBefore10()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPermissionAfter10() {
        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            && checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        ) {
            initOSM()
            checkLocationEnabled()
        } else {
            pLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            )
        }
    }

    private fun checkPermissionBefore10() {
        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            initOSM()
            checkLocationEnabled()
        } else {
            pLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            )
        }
    }

    private fun checkLocationEnabled() {
        val lManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isEnabled = lManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!isEnabled) {
            DialogManager.showLocEnabledDialog(
                activity as AppCompatActivity,
                object : DialogManager.Listener {
                    override fun onClick() {
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                }
            )
        } else {
            Toast.makeText(activity, "Location enabled", Toast.LENGTH_SHORT).show()
        }
    }


    @Suppress("UNCHECKED_CAST")
    fun <T : Serializable?> getSerializable(intent: Intent, key: String, m_class: Class<T>): T {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getSerializableExtra(key, m_class)!!
        else
            @Suppress("DEPRECATION") intent.getSerializableExtra(key) as T
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationService.LOC_MODEL_INTENT) {
                val locModel = getSerializable(
                    intent,
                    LocationService.LOC_MODEL_INTENT,
                    LocationModel::class.java
                )
                model.locationUpdates.value = locModel
            }
        }
    }

    private fun registerLocReceiver() {
        val locFilter = IntentFilter(LocationService.LOC_MODEL_INTENT)
        LocalBroadcastManager.getInstance(activity as AppCompatActivity)
            .registerReceiver(receiver, locFilter)
    }

    private fun addPoint(list: List<GeoPoint>) {
        if (list.isNotEmpty()) polyLine?.addPoint(list[list.size - 1])
    }

    private fun fillPolyLine(list: List<GeoPoint>) {
        list.forEach {
            polyLine?.addPoint(it)
        }
    }

    private fun updatePolyLine(list: List<GeoPoint>) {
        if (list.size > 1 && firstStart) {
            fillPolyLine(list)
            firstStart = false
        } else {
            addPoint(list)
        }
    }

    override fun onDetach() {
        super.onDetach()
        LocalBroadcastManager.getInstance(activity as AppCompatActivity)
            .unregisterReceiver(receiver)

    }

    companion object {
        @JvmStatic
        fun newInstance() = MainFragment()
    }
}
