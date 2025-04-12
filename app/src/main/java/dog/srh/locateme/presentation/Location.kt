package dog.srh.locateme.presentation

import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate

data class EastingNorthing(val easting: Double, val northing: Double) {
    private val osPrefixes = listOf(
        listOf("SV", "SW", "SX", "SY", "SZ", "TV", "TW"),
        listOf("SQ", "SR", "SS", "ST", "SU", "TQ", "TR"),
        listOf("SL", "SM", "SN", "SO", "SP", "TL", "TM"),
        listOf("SF", "SG", "SH", "SJ", "SK", "TF", "TG"),
        listOf("SA", "SB", "SC", "SD", "SE", "TA", "TB"),
        listOf("NV", "NW", "NX", "NY", "NZ", "OV", "OW"),
        listOf("NQ", "NR", "NS", "NT", "NU", "OQ", "OR"),
        listOf("NL", "NM", "NN", "NO", "NP", "OL", "OM"),
        listOf("NF", "NG", "NH", "NJ", "NK", "OF", "OG"),
        listOf("NA", "NB", "NC", "ND", "NE", "OA", "OB"),
        listOf("HV", "HW", "HX", "HY", "HZ", "JV", "JW"),
        listOf("HQ", "HR", "HS", "HT", "HU", "JQ", "JR"),
        listOf("HL", "HM", "HN", "HO", "HP", "JL", "JM")
    )

    val osPrefix
        get() = osPrefixes[(easting / 100000).toInt()][(northing / 100000).toInt()]

    val osNorthing
        get() = ((northing.toInt() % 100000) / 100).toString().padStart(3,'0')

    val osEasting
        get() = ((easting.toInt() % 100000) / 100).toString().padStart(3, '0')
}

data class Wgs84Coordinate(val lng: Double, val lat: Double) {
    private val wgsToOsgb by lazy {
        val crsFactory = CRSFactory()
        val wgs84 = crsFactory.createFromName("EPSG:4326")
        val osgb36 = crsFactory.createFromName("EPSG:27700")

        val coordinateTransformFactory = CoordinateTransformFactory()

        coordinateTransformFactory.createTransform(wgs84, osgb36)!!
    }

    fun toEastingNorthing(): EastingNorthing {
        val coord = ProjCoordinate()
        wgsToOsgb.transform(ProjCoordinate(this.lng, this.lat), coord)

        return EastingNorthing(coord.x, coord.y)
    }
}

val osMaxBound = Wgs84Coordinate(1.96, 60.9)
val osMinBound = Wgs84Coordinate(-8.74, 49.84)

fun checkBounds(coordinate: Wgs84Coordinate) =
    coordinate.lng < osMaxBound.lng
            && coordinate.lat < osMaxBound.lat
            && coordinate.lng > osMinBound.lng
            && coordinate.lat > osMinBound.lat
