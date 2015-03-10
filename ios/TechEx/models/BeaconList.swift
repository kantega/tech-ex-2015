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
    
    func nearest() -> CLBeacon? {
        if beacons.isEmpty {
            return Optional<CLBeacon>()
        }
        return beacons[0]
    }
    
    
    func hasLeftAllRegions() -> Bool {
        return beacons.count == 0
    }
    
    func currentRegions() -> Array<Int> {
        var regions = Array<Int>()
        for b in beacons {
            if !contains(regions, b.major.integerValue) {
                regions.append(b.major.integerValue)
            }
        }
        return regions
    }
}
