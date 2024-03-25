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

struct ContentView: View {
    @StateObject private var viewModel = ContentViewModel()

    var body: some View {
        ZStack {
            MapView(map: viewModel.map, permissionState: viewModel.permissionState)
            
            VStack {
                Spacer()
                Button(action: {
                    viewModel.findMyLocation()
                }) {
                    Text("В Москву")
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                }
                .padding(.bottom)
            }
        }
    }
}

class ContentViewModel: ObservableObject {
    @Published var map = GoogleMapShared(mapView: MKMapView())
    var permissionState = PermissionStateSharedImpl(locationManager: CLLocationManager())

    func findMyLocation() {

    }
}
