//
//  LocationDeletage.swift
//  TechEx
//
//  Created by Kristian Lier Selnæs on 11/02/15.
//  Copyright (c) 2015 Technoport. All rights reserved.
//

import Foundation
import CoreLocation

class LocationService: NSObject, CLLocationManagerDelegate {
    
    var baseApiUrl = ""
    var playerId = "";
    let locationManager = CLLocationManager()
    let region = CLBeaconRegion(proximityUUID: NSUUID(UUIDString: "f7826da6-4fa2-4e98-8024-bc5b71e0893e"), identifier: "kontakt.io")
    var lastBeacon: CLBeacon?
    var lastProximity: CLProximity?
  
    func startDetectingBeacons() {
        baseApiUrl = NSBundle.mainBundle().objectForInfoDictionaryKey("serverUrl") as String
        
        locationManager.delegate = self
        playerId = KeychainService.load(.PlayerId)!
        if (CLLocationManager.authorizationStatus() != CLAuthorizationStatus.Authorized) {
            println("Requesting authorization")
            // The 'when in use' authorization only allows you to use location services while the app is in the foreground,
            // whereas the 'always' authorization lets you access location services at any time, even waking up and starting the app in response to some event
            locationManager.requestAlwaysAuthorization()
        }
        println("Start ranging beacons")
        locationManager.startMonitoringForRegion(region)
        locationManager.startRangingBeaconsInRegion(region as CLBeaconRegion)
     }

    
    func locationManager(manager: CLLocationManager!, didEnterRegion region: CLRegion!) {
        NSLog("Device entered region \(region.description)")
        locationManager.startRangingBeaconsInRegion(region as CLBeaconRegion)
    }
    
    func locationManager(manager: CLLocationManager!, didExitRegion region: CLRegion!) {
        
    }
    
    func locationManager(manager: CLLocationManager!, didRangeBeacons beacons: [AnyObject]!, inRegion region: CLBeaconRegion!) {

        let knownBeacons = beacons.filter{ $0.proximity != CLProximity.Unknown }
        if (knownBeacons.count > 0) {
            let closestBeacon = knownBeacons[0] as CLBeacon
            //println("Beacon detected. Minor: \(closestBeacon.minor). Proximity: \(closestBeacon.proximity.rawValue). \(closestBeacon)")
            if closestBeacon.minor != lastBeacon?.minor || closestBeacon.proximity != lastProximity {
                self.lastBeacon = closestBeacon
                self.lastProximity = closestBeacon.proximity

                let parameters = [
                    "beaconId": "\(closestBeacon.major):\(closestBeacon.minor)",
                    "proximity": "\(closestBeacon.proximity.rawValue)"
                ];
                NSLog("Sending beacon data to server. Parameters: \(parameters)")
                
                request(.POST, "\(baseApiUrl)/location/\(playerId)", parameters: parameters, encoding: .JSON)
                   .responseString { (req, resp, s, error) in
                        if error != nil {                            
                            NSLog("Error when reporting location: \(error)");
                        } else {
                            println(s)
                    }
                }
            }
        }
    }

    
    
}


