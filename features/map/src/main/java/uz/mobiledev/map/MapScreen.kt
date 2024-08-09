package uz.mobiledev.map

import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.viewinterop.NoOpUpdate
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.scalebar.scalebar
import kotlinx.coroutines.launch

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    onPermissionGranted: () -> Unit
) {
    MapMainScreen(
        modifier = modifier,
        onPermissionGranted = {
            onPermissionGranted()
        }
    )
}

@SuppressLint("MissingPermission")
@Composable
internal fun MapMainScreen(
    modifier: Modifier = Modifier,
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current

    var point: Point? by remember {
        mutableStateOf(null)
    }

    var relaunch by remember {
        mutableStateOf(false)
    }

    var zoomLevel by remember {
        mutableDoubleStateOf(16.0)
    }

    var isLocationPermissionGranted by remember {
        mutableStateOf(false)
    }

    MapBox(
        point = point,
        modifier = modifier
            .fillMaxSize(),
        levelOfZoom = zoomLevel
    )

    BottomSheet(
        modifier,
        onNavigationClick = {
            relaunch = !relaunch
            zoomLevel = 16.0
        },
        onZoomInClick = {
            zoomLevel += 1
        },
        onZoomOutClick = {
            if (zoomLevel > 10.0) zoomLevel -= 1
        }
    )

    val permissionRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (!permissions.values.all { it }) {
                //handle permission denied
            } else {
                relaunch = !relaunch
                isLocationPermissionGranted = true
            }
        }
    )

    LaunchedEffect(key1 = relaunch) {
        try {
            val location = LocationService().getCurrentLocation(context)
            point = Point.fromLngLat(location.longitude, location.latitude)
            onPermissionGranted()
        } catch (e: LocationService.LocationServiceException) {
            when (e) {
                is LocationService.LocationServiceException.LocationDisabledException -> {
                    //handle location disabled, show dialog or a snack-bar to enable location
                }

                is LocationService.LocationServiceException.MissingPermissionException -> {
                    permissionRequest.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }

                is LocationService.LocationServiceException.NoNetworkEnabledException -> {
                    //handle no network enabled, show dialog or a snack-bar to enable network
                }

                is LocationService.LocationServiceException.UnknownException -> {
                    //handle unknown exception
                }
            }
        }
    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BottomSheet(
    modifier: Modifier,
    onNavigationClick: () -> Unit,
    onZoomInClick: () -> Unit,
    onZoomOutClick: () -> Unit,
) {
    var isExpanded by remember {
        mutableStateOf(false)
    }
    var expandClicked: Boolean? by remember {
        mutableStateOf(null)
    }
    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        modifier = Modifier,
        scaffoldState = scaffoldState,
        sheetPeekHeight = 150.dp,
        sheetContainerColor = Color.Transparent,
        sheetShadowElevation = 0.dp,
        contentColor = Color.Transparent,
        sheetContent = {
            Card(
                modifier = Modifier
                    .defaultMinSize(minHeight = 230.dp),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                colors = CardColors(
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.onSecondary,
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.onSecondary
                ),
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp, 16.dp, 16.dp, 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardColors(
                        MaterialTheme.colorScheme.onBackground,
                        MaterialTheme.colorScheme.onSecondary,
                        MaterialTheme.colorScheme.onBackground,
                        MaterialTheme.colorScheme.onSecondary,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                    ) {
                        MenuItem(
                            icon = R.drawable.ic_tariff,
                            title = "Tarif",
                            info = "6 / 8",
                            onClick = { /* TODO: Handle click */ }
                        )
                        Divider(
                            color = Color.LightGray,
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        MenuItem(
                            icon = R.drawable.ic_order,
                            title = "Buyurtmalar",
                            info = "0",
                            onClick = { /* TODO: Handle click */ }
                        )
                        Divider(
                            color = Color.LightGray,
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        MenuItem(
                            icon = R.drawable.ic_rocket,
                            title = "Bordur",
                            info = "",
                            onClick = { /* TODO: Handle click */ }
                        )
                    }
                }
            }

        }, content = { innerPadding ->

        }
    )
    LaunchedEffect(key1 = scaffoldState.bottomSheetState.currentValue) {
        isExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
    }
    LaunchedEffect(key1 = expandClicked) {
        Log.d("XXX","expandClicked")
        if (expandClicked != null) scaffoldState.bottomSheetState.expand()
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
    ) {
        AppBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
        )

        if (!isExpanded) {
            UpArrowButton(
                onClick = {
                    expandClicked?.let {
                        expandClicked = !it
                    }?:run {
                        expandClicked = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
            )
            MapButtons(
                modifier = Modifier
                    .align(Alignment.CenterEnd),
                onNavigationClick = {
                    onNavigationClick()
                },
                onZoomInClick = {
                    onZoomInClick()
                },
                onZoomOutClick = {
                    onZoomOutClick()
                }
            )
        }
    }
}

@Composable
internal fun MapBox(
    modifier: Modifier = Modifier,
    point: Point?,
    levelOfZoom: Double
) {
    val context = LocalContext.current

    val marker = remember(context) {
        context.getDrawable(R.drawable.ic_car)!!.toBitmap()
    }
    val isDarkTheme = isSystemInDarkTheme()

    var pointAnnotationManager: PointAnnotationManager? by remember {
        mutableStateOf(null)
    }

    AndroidView(
        factory = {
            MapView(it).also { mapView ->
                val styleUri = if (isDarkTheme) {
                    Style.DARK
                } else {
                    Style.TRAFFIC_DAY
                }
                mapView.getMapboxMap().loadStyleUri(styleUri)
                mapView.compass.enabled = false
                mapView.scalebar.enabled = false
                val annotationApi = mapView.annotations
                pointAnnotationManager = annotationApi.createPointAnnotationManager()

//                mapView.getMapboxMap().addOnMapClickListener { p ->
//                    onPointChange(p)
//                    true
//                }
                val defaultLocation = Point.fromLngLat(41.3052887, 69.2758443)
                mapView.getMapboxMap()
                    .flyTo(CameraOptions.Builder().zoom(levelOfZoom).center(defaultLocation).build())
            }
        },
        update = { mapView ->
            if (point != null) {
                pointAnnotationManager?.let {
                    it.deleteAll()
                    val pointAnnotationOptions = PointAnnotationOptions()
                        .withPoint(point)
                        .withIconImage(marker)

                    it.create(pointAnnotationOptions)
                    mapView.getMapboxMap()
                        .flyTo(CameraOptions.Builder().zoom(levelOfZoom).center(point).build())
                }
            }
            NoOpUpdate
        },
        modifier = modifier
    )
}

@Composable
internal fun AppBar(
    modifier: Modifier
) {
    val isActive = remember {
        mutableStateOf(true)
    }

    Row(
        modifier = modifier
            .defaultMinSize(minWidth = 328.dp)
            .padding(top = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        RoundedButton(
            modifier = Modifier,
            size = 56,
            corner = 14,
            colors = CardColors(
                MaterialTheme.colorScheme.onPrimary,
                MaterialTheme.colorScheme.onSecondary,
                MaterialTheme.colorScheme.onPrimary,
                MaterialTheme.colorScheme.onSecondary
            ),
            iconSize = 24,
            iconSource = R.drawable.ic_frame,
            iconColor = MaterialTheme.colorScheme.onSecondary,
            onClick = { }
        )

        Card(
            modifier = Modifier
                .width(192.dp)
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardColors(
                MaterialTheme.colorScheme.onPrimary,
                MaterialTheme.colorScheme.onSecondary,
                MaterialTheme.colorScheme.onPrimary,
                MaterialTheme.colorScheme.onSecondary
            ),
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { isActive.value = false },
                    modifier = Modifier
                        .height(48.dp)
                        .width(92.dp)
                        .weight(0.95f)
                        .align(Alignment.CenterVertically),
                    shape = RoundedCornerShape(10.dp),
                    colors =
                    if (!isActive.value) {
                        ButtonDefaults.buttonColors(
                            Color(0xFFFA5255),
                            Color.Black
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            MaterialTheme.colorScheme.onPrimary,
                            MaterialTheme.colorScheme.onSecondary
                        )
                    }
                ) {
                    Text(
                        text = "Band",
                        fontSize = 17.sp,
                        fontStyle = FontStyle.Normal,
                        fontWeight = FontWeight(if (!isActive.value) 700 else 400),
                        fontFamily = FontFamily(Font(R.font.lato))
                    )
                }
                Button(
                    onClick = { isActive.value = true },
                    modifier = Modifier
                        .height(48.dp)
                        .width(92.dp)
                        .weight(0.95f)
                        .align(Alignment.CenterVertically),
                    shape = RoundedCornerShape(10.dp),
                    colors =
                    if (isActive.value) {
                        ButtonDefaults.buttonColors(
                            Color(0xFF80ED99),
                            Color.Black
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            MaterialTheme.colorScheme.onPrimary,
                            MaterialTheme.colorScheme.onSecondary
                        )
                    }
                ) {
                    Text(
                        text = "Faol",
                        fontSize = 17.sp,
                        fontStyle = FontStyle.Normal,
                        fontWeight = FontWeight(if (isActive.value) 700 else 400),
                        fontFamily = FontFamily(Font(R.font.lato))
                    )
                }
            }
        }

        Card(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardColors(Color(0xFF80ED99), Color.Black, Color(0xFF80ED99), Color.Black),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusable(true)
                    .clickable { }
                    .border(
                        4.dp,
                        MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "95",
                    fontSize = 22.sp,
                    color = Color.Black,
                    fontStyle = FontStyle.Normal,
                    fontWeight = FontWeight(700),
                    fontFamily = FontFamily(Font(R.font.lato))
                )
            }
        }
    }
}

@Composable
internal fun UpArrowButton(onClick: () -> Unit, modifier: Modifier = Modifier) {

    Surface(
        modifier = modifier
            .padding(start = 16.dp)
            .size(56.dp)
            .border(4.dp, MaterialTheme.colorScheme.background, shape = RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        onClick = { onClick() },
    ) {

        RoundedButton(
            modifier = modifier,
            size = 56,
            corner = 14,
            colors = CardColors(
                MaterialTheme.colorScheme.onTertiary,
                MaterialTheme.colorScheme.onSecondary,
                MaterialTheme.colorScheme.onPrimary,
                MaterialTheme.colorScheme.onSecondary,
            ),
            iconSize = 24,
            iconSource = R.drawable.ic_chevrons,
            iconColor = MaterialTheme.colorScheme.surface,
            onClick = { onClick() }
        )
    }
}

@Composable
internal fun MapButtons(
    modifier: Modifier = Modifier,
    onZoomInClick: () -> Unit,
    onZoomOutClick: () -> Unit,
    onNavigationClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .height(200.dp)
            .padding(end = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            modifier = modifier.size(56.dp),
            color = Color(0xFFFFFFFF),
            shape = RoundedCornerShape(14.dp),
        ) {

            RoundedButton(
                modifier = Modifier,
                size = 56,
                corner = 14,
                colors = CardColors(
                    MaterialTheme.colorScheme.onPrimary,
                    MaterialTheme.colorScheme.onSecondary,
                    MaterialTheme.colorScheme.onPrimary,
                    MaterialTheme.colorScheme.onSecondary,
                ),
                iconSize = 24,
                iconSource = R.drawable.ic_plus,
                iconColor = MaterialTheme.colorScheme.surface,
                onClick = { onZoomInClick() }
            )
        }

        RoundedButton(
            modifier = Modifier,
            size = 56,
            corner = 14,
            colors = CardColors(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.onSecondary,
                MaterialTheme.colorScheme.onPrimary,
                MaterialTheme.colorScheme.onSecondary,
            ),
            iconSize = 24,
            iconSource = R.drawable.ic_minus,
            iconColor = MaterialTheme.colorScheme.surface,
            onClick = { onZoomOutClick() }
        )

        RoundedButton(
            modifier = Modifier,
            size = 56,
            corner = 14,
            colors = CardColors(
                MaterialTheme.colorScheme.onPrimary,
                MaterialTheme.colorScheme.onSecondary,
                MaterialTheme.colorScheme.onPrimary,
                MaterialTheme.colorScheme.onSecondary,
            ),
            iconSize = 24,
            iconSource = R.drawable.ic_navigation,
            iconColor = Color(0xFF4A91FB),
            onClick = { onNavigationClick() }
        )

    }
}

@Composable
internal fun MenuItem(icon: Int, title: String, info: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.surface
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            fontFamily = FontFamily(Font(R.font.lato)),
            color = MaterialTheme.colorScheme.onSecondary,
        )
        Text(
            text = info,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily(Font(R.font.lato)),
            color = MaterialTheme.colorScheme.surface
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_frame_arrow),
            contentDescription = "Next",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.surface,
        )
    }
}

@Composable
fun RoundedButton(
    modifier: Modifier,
    size: Int,
    corner: Int,
    colors: CardColors,
    iconSize: Int,
    iconSource: Int,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.size(size.dp),
        shape = RoundedCornerShape(corner.dp),
        colors = colors,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(iconSize.dp),
                painter = painterResource(id = iconSource),
                tint = iconColor,
                contentDescription = null
            )
        }
    }
}
