package com.example.bletestapp

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bletestapp.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.maps.GeoApiContext
import com.google.maps.RoadsApi
import com.google.maps.model.SnappedPoint
import data.SnapToRoadsResponse
import kotlinx.coroutines.*
import okhttp3.*
import java.io.IOException


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // マップ上にルートを描画する座標を設定
        getRouteCoordinates()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun getRouteCoordinates() {
        var snappedPoints = ArrayList<LatLng>()
        val coordinates: MutableList<com.google.maps.model.LatLng> = mutableListOf()
        // test data
        coordinates.add(com.google.maps.model.LatLng(-35.27801, 149.12958))
        coordinates.add(com.google.maps.model.LatLng(-35.28032, 149.12907))
        coordinates.add(com.google.maps.model.LatLng(-35.28099, 149.12929))
        coordinates.add(com.google.maps.model.LatLng(-35.28144, 149.12984))
        coordinates.add(com.google.maps.model.LatLng(-35.28194, 149.13003))
        coordinates.add(com.google.maps.model.LatLng(-35.28282, 149.12956))
        coordinates.add(com.google.maps.model.LatLng(-35.28302, 149.12881))
        coordinates.add(com.google.maps.model.LatLng(-35.28473, 149.12836))


        GlobalScope.launch(Dispatchers.IO) {
            snappedPoints = fetchSnappedPointsFromRoadApi(coordinates.toTypedArray())
            withContext(Dispatchers.Main) {
                drawRoute(snappedPoints)
            }
        }


//        val coordinates = "-35.27801%2C149.12958%7C-35.28032%2C149.12907%7C-35.28099%2C149.12929%7C" +
//                "-35.28144%2C149.12984%7C-35.28194%2C149.13003%7C-35.28282%2C149.12956%7C" +
//                "-35.28302%2C149.12881%7C-35.28473%2C149.12836"
//        val client = OkHttpClient()
//        val url = "https://roads.googleapis.com/v1/snapToRoads?path=$coordinates&key=${BuildConfig.GOOGLE_API_KEY}"
//
//        val request = Request.Builder()
//            .url(url)
//            .build()
//
//        client.newCall(request).enqueue(object: Callback {
//            override fun onFailure(call: Call?, e: IOException?) {
//                runOnUiThread {
//                    Toast.makeText(
//                        applicationContext,
//                        "Failed to show route data! Error ${e.toString()}", Toast.LENGTH_LONG
//                    ).show()
//                }
//            }
//
//            override fun onResponse(call: Call?, response: Response?) {
//                val responseBody = response?.body()?.string()
//                val routeCoordinates = ArrayList<LatLng>()
//                responseBody.also { it ->
//                    val snapToRoadsResponse = Gson().fromJson(it, SnapToRoadsResponse::class.java)
//                    // SnappedPointからLatLngに変換
//                    val snappedPoints = snapToRoadsResponse.snappedPoints.map { it.toLatLng() }
//                    if (snappedPoints.isEmpty()) return
//                    runOnUiThread {
//                        // ルートを描画
//                        val polylineOptions = PolylineOptions()
//                            .addAll(snappedPoints)
//                            .color(Color.BLUE)
//                            .width(5f)
//                        mMap.addPolyline(polylineOptions)
//
//                        // ズームレベルとカメラ位置を設定
//                        if (snappedPoints.isEmpty()) return@runOnUiThread
//                        val boundsBuilder = LatLngBounds.Builder()
//                        snappedPoints.forEach {
//                            boundsBuilder.include(it)
//                        }
//                        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100)
//                        mMap.moveCamera(cameraUpdate)
//                    }
//                }
//            }
//        })

    }

    private fun drawRoute(snappedPoints: ArrayList<LatLng>) {
        // ルートを描画
        val polylineOptions = PolylineOptions()
            .addAll(snappedPoints)
            .color(Color.BLUE)
            .width(8f)
        mMap.addPolyline(polylineOptions)
        mMap.addMarker(MarkerOptions().position(snappedPoints[0]).title("Start"))
        mMap.addMarker(MarkerOptions().position(snappedPoints.last()).title("End")
            .snippet("walk count: ${999}")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))

        // ズームレベルとカメラ位置を設定
        val boundsBuilder = LatLngBounds.Builder()
        snappedPoints.forEach {
            boundsBuilder.include(it)
        }
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120)
        mMap.moveCamera(cameraUpdate)
    }

    /**
     *  Get snapped points from Roads API
     *
     */
    private suspend fun fetchSnappedPointsFromRoadApi(path: Array<com.google.maps.model.LatLng>): ArrayList<LatLng> {
        var snappedPoints = ArrayList<LatLng>()
        val context = GeoApiContext.Builder()
            .apiKey(BuildConfig.GOOGLE_API_KEY)
            .build()

        try {
            // SnapToRoads APIを使用してルート座標を取得
            val snappedPointList = RoadsApi.snapToRoads(context, true, *path).await()
            for (snappedPoint in snappedPointList) {
                snappedPoints.add(LatLng(snappedPoint.location.lat, snappedPoint.location.lng))
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(
                    applicationContext,
                    "Failed to show route data! Error $e", Toast.LENGTH_LONG
                ).show()
            }
        }
        return snappedPoints
    }

    /**
     * Snaps the points to their most likely position on roads using the Roads API.
     * https://developers.google.com/maps/documentation/roads/advanced?hl=ja
     */
    @Throws(java.lang.Exception::class)
    private fun snapToRoads(context: GeoApiContext, capturedLocations: List<com.google.maps.model.LatLng>): List<SnappedPoint>? {
        if (capturedLocations.isEmpty()) return null
        val snappedPoints: MutableList<SnappedPoint> = mutableListOf()
        var offset = 0
        while (offset < capturedLocations.size) {
            // Calculate which points to include in this request. We can't exceed the API's
            // maximum and we want to ensure some overlap so the API can infer a good location for
            // the first few points in each request.
            if (offset > 0) {
                offset -= PAGINATION_OVERLAP // Rewind to include some previous points.
            }
            val lowerBound = offset
            val upperBound = (offset + PAGE_SIZE_LIMIT).coerceAtMost(capturedLocations.size)

            // Get the data we need for this page.
            val page: Array<com.google.maps.model.LatLng> = capturedLocations
                .subList(lowerBound, upperBound)
                .toTypedArray()

            // Perform the request. Because we have interpolate=true, we will get extra data points
            // between our originally requested path. To ensure we can concatenate these points, we
            // only start adding once we've hit the first new point (that is, skip the overlap).
            val points: Array<SnappedPoint> = RoadsApi.snapToRoads(context, true, *page).await()
            var passedOverlap = false
            for (point in points) {
                if (offset == 0 || point.originalIndex >= PAGINATION_OVERLAP - 1) {
                    passedOverlap = true
                }
                if (passedOverlap) {
                    snappedPoints.add(point)
                }
            }
            offset = upperBound
        }
        return snappedPoints
    }

    companion object {
        /**
         * The number of points allowed per API request. This is a fixed value.
         */
        private const val PAGE_SIZE_LIMIT = 100

        /**
         * Define the number of data points to re-send at the start of subsequent requests. This helps
         * to influence the API with prior data, so that paths can be inferred across multiple requests.
         * You should experiment with this value for your use-case.
         */
        private const val PAGINATION_OVERLAP = 5
    }
}
