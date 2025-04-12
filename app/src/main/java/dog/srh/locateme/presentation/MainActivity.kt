package dog.srh.locateme.presentation

import android.annotation.SuppressLint
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.angularSize
import androidx.wear.compose.foundation.background
import androidx.wear.compose.foundation.curvedBox
import androidx.wear.compose.foundation.radialSize
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import dog.srh.locateme.presentation.theme.DarkTheme
import dog.srh.locateme.presentation.theme.LightTheme
import dog.srh.locateme.presentation.theme.onErrorDark
import dog.srh.locateme.presentation.theme.onTertiaryDark
import dog.srh.locateme.presentation.theme.tertiaryDark
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.DeviceOrientation
import com.google.android.gms.location.DeviceOrientationRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import kotlin.math.absoluteValue
import kotlin.math.sign

class MainActivity : ComponentActivity() {
    @SuppressLint("MissingPermission")
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val locationPermissions = rememberMultiplePermissionsState(
                listOf(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
            var heading by remember { mutableFloatStateOf(0f) }
            var headingError by remember { mutableFloatStateOf(180f) }
            var pitch by remember { mutableFloatStateOf(0f) }
            var currentLocation by remember { mutableStateOf<Location?>(null) }

            DisposableEffect(context) {
                val fop = LocationServices.getFusedOrientationProviderClient(context)
                val rotationMatrix = FloatArray(9)
                val orientation = FloatArray(3)
                val listener = { it: DeviceOrientation ->
                    heading = it.headingDegrees
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, it.attitude)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                    headingError = if (it.hasConservativeHeadingErrorDegrees()) {
                        it.conservativeHeadingErrorDegrees
                    } else {
                        it.headingErrorDegrees
                    }
                }

                fop.requestOrientationUpdates(
                    DeviceOrientationRequest.Builder(
                        DeviceOrientationRequest.OUTPUT_PERIOD_DEFAULT
                    ).build(),
                    ContextCompat.getMainExecutor(context),
                    listener
                )

                onDispose {
                    fop.removeOrientationUpdates(listener)
                }
            }

            DisposableEffect(locationPermissions.allPermissionsGranted, context) {
                if (!locationPermissions.allPermissionsGranted) return@DisposableEffect onDispose {}

                val fls = LocationServices.getFusedLocationProviderClient(context)
                val listener = { it: Location ->
                    currentLocation = it
                }

                fls.requestLocationUpdates(
                    LocationRequest.Builder(
                        120_000
                    ).build(),
                    ContextCompat.getMainExecutor(context),
                    listener
                )

                onDispose {
                    fls.removeLocationUpdates(listener)
                }
            }

            WearApp(
                heading = heading,
                headingError = headingError,
                pitch = pitch,
                currentLocation = currentLocation,
                hasPermissions = locationPermissions.allPermissionsGranted,
                requestPermissions = {
                    locationPermissions.launchMultiplePermissionRequest()
                },
                requestImmediateLocationUpdate = {
                    currentLocation = null
                }
            )
        }
    }
}

@Composable
fun UnsupportedApp() {
    DarkTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = "Your watch doesn't support this app",
            )
        }
    }
}

@Composable
fun minutesPassed(from: Long): Long {
    var minutes by remember {
        mutableLongStateOf((SystemClock.elapsedRealtime() - from) / 60_000)
    }

    LaunchedEffect(from) {
        minutes = (SystemClock.elapsedRealtime() - from) / 60_000

        while (true) {
            delay((SystemClock.elapsedRealtime() - from) % 60_000)
            minutes = (SystemClock.elapsedRealtime() - from) / 60_000
        }
    }

    return minutes
}

@Composable
fun currentTime(): String {
    var timeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay((System.currentTimeMillis()) % 60_000)
            timeMillis = (System.currentTimeMillis()) / 60_000
        }
    }

    val instant = Instant.ofEpochMilli(timeMillis)
    val tzDateTime = instant.atZone(ZoneId.systemDefault())

    return "${tzDateTime.hour.toString().padStart(2, '0')}:${tzDateTime.minute.toString().padStart(2, '0')}"
}

@Composable
fun Statistic(
    modifier: Modifier,
    heading: Float,
    topText: String,
    bottomText: String
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(48.dp)
            .clipToBounds()
            .zIndex(1f)
    ) {
        LightTheme {
            CompassRim(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.Center)
                    .zIndex(1f)
                    .rotate(heading)
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(
                    bottomEnd = 4.dp,
                    bottomStart = 4.dp
                ))
                .fillMaxWidth()
                .height(32.dp)
                .align(Alignment.TopCenter)
                .background(MaterialTheme.colors.primary)
                .zIndex(0f)
                .padding(bottom = 1.dp)
        ) {
            Text(
                text = topText,
                color = MaterialTheme.colors.onPrimary,
                style = MaterialTheme.typography.caption2,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(
                    topEnd = 4.dp,
                    topStart = 4.dp
                ))
                .fillMaxWidth()
                .height(32.dp)
                .align(Alignment.BottomCenter)
                .background(MaterialTheme.colors.primary)
                .zIndex(0f)
                .padding(top = 1.dp)
        ) {
            Text(
                text = bottomText,
                color = MaterialTheme.colors.onPrimary,
                style = MaterialTheme.typography.caption2,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun WearApp(
    heading: Float,
    headingError: Float,
    pitch: Float,
    currentLocation: Location?,
    hasPermissions: Boolean,
    requestPermissions: () -> Unit,
    requestImmediateLocationUpdate: () -> Unit
) {
    val minutesSinceLastLocation = currentLocation?.let {
        minutesPassed(it.elapsedRealtimeMillis)
    }
    var unwrappedAngleTarget by remember { mutableFloatStateOf(0f) }
    var gridReference by remember { mutableStateOf("__ ___ ___") }
    var hasComputedGridReference by remember { mutableStateOf(false) }

    LaunchedEffect(currentLocation) {
        if (currentLocation == null) return@LaunchedEffect

        gridReference = withContext(Dispatchers.IO) {
            hasComputedGridReference = false

            val eastingNorthing = Wgs84Coordinate(
                currentLocation.longitude,
                currentLocation.latitude
            )
                .toEastingNorthing()

            "${eastingNorthing.osPrefix} ${eastingNorthing.osEasting} ${eastingNorthing.osNorthing}"
        }

        hasComputedGridReference = true
    }

    LaunchedEffect(heading) {
        val currentApparentAngle = unwrappedAngleTarget.mod(360f)
        var diff = heading - currentApparentAngle

        diff = (diff + 180f).mod(360f) - 180f

        unwrappedAngleTarget += diff
    }

    val animatedHeading = animateFloatAsState(
        targetValue = unwrappedAngleTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    DarkTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            Column (
                modifier = Modifier
                    .align(Alignment.Center)
            ) {
                if (headingError > 5f) {
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(4.dp))
                            .background(when (headingError) {
                                in 5f..<180f -> MaterialTheme.colors.primary
                                else -> MaterialTheme.colors.error
                            })
                            .padding(horizontal = 4.dp),
                        textAlign = TextAlign.Center,
                        text = when (headingError) {
                            180f -> "UNRELIABLE"
                            in 90f..<180f -> "INACCURATE"
                            in 45f..90f -> "LOW ACCURACY"
                            in 5f..45f -> "MED ACCURACY"
                            else -> "UNKNOWN"
                        },
                        color = when (headingError) {
                            in 5f..<180f -> MaterialTheme.colors.onPrimary
                            else -> MaterialTheme.colors.onError
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = gridReference,
                    style = MaterialTheme.typography.title1
                )

                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(4.dp))
                        .background(when {
                            !hasPermissions -> MaterialTheme.colors.error
                            currentLocation == null -> MaterialTheme.colors.secondary
                            !hasComputedGridReference -> MaterialTheme.colors.primary
                            minutesSinceLastLocation!! > 1 -> MaterialTheme.colors.primary
                            else -> tertiaryDark
                        },)
                        .padding(horizontal = 4.dp)
                        .let {
                            when (hasPermissions) {
                                false -> it.clickable(
                                    role = Role.Button,
                                    onClickLabel = "Grant GPS permissions"
                                ) {
                                    requestPermissions()
                                }
                                true -> it
                            }
                        },
                    textAlign = TextAlign.Center,
                    text = when {
                        !hasPermissions -> "NO PERMISSION"
                        currentLocation == null -> "NO LOCATION"
                        !hasComputedGridReference -> "COMPUTING"
                        minutesSinceLastLocation!! > 1 -> "$minutesSinceLastLocation MIN AGO"
                        else -> "GPS ACTIVE"
                    },
                    color = when {
                        !hasPermissions -> MaterialTheme.colors.onError
                        currentLocation == null -> MaterialTheme.colors.onSecondary
                        !hasComputedGridReference -> MaterialTheme.colors.onPrimary
                        minutesSinceLastLocation!! > 1 -> MaterialTheme.colors.onPrimary
                        else -> onTertiaryDark
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            CurvedLayout {
                curvedBox(
                    modifier = CurvedModifier
                        .background(onErrorDark)
                        .angularSize(headingError * 2)
                        .radialSize(8.dp)
                ) {}
            }

            CompassRim(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f)
                    .rotate(-animatedHeading.value)
            )

            Statistic(
                modifier = Modifier
                    .fillMaxHeight(),
                topText = currentTime(),
                bottomText = "${heading.toInt().toString().padStart(3, '0')}°",
                heading = -animatedHeading.value
            )

            Statistic(
                modifier = Modifier
                    .fillMaxHeight()
                    .rotate(90f),
                topText = when {
                    currentLocation == null -> "---"
                    !currentLocation.hasAltitude() -> "---"
                    else -> "${currentLocation.altitude.toInt()} m"
                },
                bottomText = "${if (pitch.sign < 0) {"-"} else {""}}${pitch.absoluteValue.toInt().toString().padStart(3, '0')}°",
                heading = -animatedHeading.value - 90
            )
        }

    }
}

@Composable
fun BoxScope.CompassMinorTickMark(rotation: Float) {
    Box (
        modifier = Modifier
            .align(Alignment.Center)
            .fillMaxHeight()
            .rotate(rotation)
    ) {
        Box (
            modifier = Modifier
                .align(Alignment.TopCenter)
                .height(10.dp)
                .width(2.dp)
                .background(color = MaterialTheme.colors.secondaryVariant)
        )

        Box (
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .height(10.dp)
                .width(2.dp)
                .background(color = MaterialTheme.colors.secondaryVariant)
        )
    }
}

@Composable
fun BoxScope.CompassMajorTickMark(rotation: Float) {
    Box (
        modifier = Modifier
            .align(Alignment.Center)
            .fillMaxHeight()
            .rotate(rotation)
    ) {
        Box (
            modifier = Modifier
                .align(Alignment.TopCenter)
                .height(14.dp)
                .width(2.dp)
                .background(color = MaterialTheme.colors.secondary)
        )

        Box (
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .height(14.dp)
                .width(2.dp)
                .background(color = MaterialTheme.colors.secondary)
        )
    }
}

@Composable
fun CompassRim(modifier: Modifier = Modifier) {
    Box (
        modifier = modifier
    ) {
        Box (
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .rotate(0f)
        ) {
            Text(
                "N",
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.TopCenter),
                color = MaterialTheme.colors.primary
            )
            Text(
                "S",
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .rotate(180f),
                color = MaterialTheme.colors.secondary
            )
        }

        Box (
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .rotate(90f)
        ) {
            Text(
                "E",
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.TopCenter),
                color = MaterialTheme.colors.secondary
            )
            Text(
                "W",
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .rotate(180f),
                color = MaterialTheme.colors.secondary
            )
        }

        for (rotation in 1..8) {
            if (rotation % 2 == 0) {
                CompassMajorTickMark(rotation * 10f)
            } else {
                CompassMinorTickMark(rotation * 10f)
            }
        }

        for (rotation in 10..17) {
            if (rotation % 2 == 0) {
                CompassMajorTickMark(rotation * 10f)
            } else {
                CompassMinorTickMark(rotation * 10f)
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp(
        heading = 13f,
        pitch = 0f,
        headingError = 36.77f,
        currentLocation = null,
        hasPermissions = false,
        requestPermissions = {},
        requestImmediateLocationUpdate = {}
    )
}