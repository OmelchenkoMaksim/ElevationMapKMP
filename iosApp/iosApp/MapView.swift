//
//  MapView.swift
//  iosApp
//
//  Created by Омельченко Максим Вячеславович on 24.03.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import MapKit
import shared

struct MapView: UIViewRepresentable {
    var map: GoogleMapShared
    var permissionState: PermissionStateSharedImpl

    func makeUIView(context: Context) -> MKMapView {
        setupMapUI(mapView: map.mapView)
        return map.mapView
    }
    
    func updateUIView(_ uiView: MKMapView, context: Context) {
        
    }

    private func setupMapUI(mapView: MKMapView) {
        mapView.showsUserLocation = true
        mapView.userTrackingMode = .follow
        handleLocationPermission(mapView: mapView)
    }

    private func handleLocationPermission(mapView: MKMapView) {
        switch permissionState.status {
        case .granted:
            mapView.showsUserLocation = true
        case .denied, .restricted:
            break
        case .unknown:
            permissionState.requestPermission()
        default:
            break
        }
    }
}

