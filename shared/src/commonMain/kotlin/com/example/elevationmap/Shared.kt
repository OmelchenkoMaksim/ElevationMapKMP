@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.example.elevationmap

/**
 * Общий интерфейс для состояния разрешений
 */
expect interface PermissionStateShared

/**
 * Общий интерфейс контекста
 * В Android это будет Context, в iOS еще думаю что с ним сделать
 */
expect interface ContextShared

/**
 * Общий класс для представления местоположения
 */
expect class LocationShared

/**
 * Общий класс для работы с картой
 * В Android это может быть GoogleMap, в iOS - MKMapView.
 */
expect class GoogleMapShared

/**
 * Установка пользовательского интерфейса карты для обеих платформ
 * (масштаб, тип карты, включение UI элементов управления и т.д.)
 *
 * @param map GoogleMapShared, пользовательский интерфейс которого необходимо настроить.
 */
expect fun setupMapUI(map: GoogleMapShared)

/**
 * Обработать разрешения для местоположения
 */
expect fun handleLocationPermission(
    map: GoogleMapShared,
    locationPermissionState: PermissionStateShared,
    context: ContextShared
)

/**
 * Найти и отобразить текущее местоположение пользователя на карте
 */
expect suspend fun findMyLocation(
    map: GoogleMapShared,
    context: ContextShared
)

/**
 * Получает последнее известное местоположение пользователя
 */
expect suspend fun getLastKnownLocation(
    context: ContextShared
): LocationShared?

/**
 * Это общие константы и определения, используемые во всём KMP
 */
object Common {

    const val ZOOM_RATE = 15f
    const val FIND_ME = "На старт (в Москву)"

    data class MapUiSettings(
        val isFindMeButtonClicked: Boolean = false
    )

    enum class PermissionStatus {
        Granted,
        Denied,
        Restricted,
        Unknown
    }
}
