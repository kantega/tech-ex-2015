//
//  File.swift
//  BeaconRanger
//
//  Created by Kristian Lier Selnæs on 05/03/15.
//  Copyright (c) 2015 Kantega. All rights reserved.
//

import Foundation
import CoreLocation

class BeaconList {
    
    var beacons:Array<CLBeacon>
    var nearestMinorId = -1
    
    
    init() {
        beacons = Array<CLBeacon>()
    }
    
    func insert(beacons: [CLBeacon]) {
        for b in beacons {
            insert(b)
        }
    }
    
    func insert(beacon: CLBeacon) {
        remove(beacon)
        beacons.append(beacon)
        beacons.sort(sortBeacons)
    }
    
    func sortBeacons(a:CLBeacon, b:CLBeacon) -> Bool {
        if a.proximity.rawValue != b.proximity.rawValue {
            return a.proximity.rawValue < b.proximity.rawValue
        }
        return a.accuracy < b.accuracy
    }
    
    func remove(beacon: CLBeacon) {
        beacons = beacons.filter{ $0.major != beacon.major || $0.minor != beacon.minor }
    }
    
    func remove(withMajor: Int) {
        beacons = beacons.filter { $0.major != withMajor }
    }
    
    func objectAtIndex(index: Int) -> CLBeacon {
        return beacons[index]
    }
    
    func count() -> Int {
        return beacons.count
    }
    
    func nearest() -> CLBeacon {
        return beacons[0]
    }
    
    func nearestHasChanged() -> Bool {
        if nearestMinorId != beacons.first?.minor.integerValue {
            nearestMinorId = beacons.first!.minor.integerValue
            return true
        } else {
            return false
        }
    }
    
}
