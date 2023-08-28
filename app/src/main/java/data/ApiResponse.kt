package data

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName

data class SnapToRoadsResponse(
    @SerializedName("snappedPoints")
    val snappedPoints: List<SnappedPoint>
)

data class SnappedPoint(
    @SerializedName("location")
    val location: Location
) {
    fun toLatLng(): LatLng {
        return LatLng(location.latitude, location.longitude)
    }
}

data class Location(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double
)