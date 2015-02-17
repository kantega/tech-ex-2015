//
//  Config.swift
//  TechEx
//
//  Created by Kristian Lier Selnæs on 12/02/15.
//  Copyright (c) 2015 Technoport. All rights reserved.
//

import Foundation

class Config: NSObject {
    
    
    
    // Looks up a property value from config.plist
    internal class func get(property: String) -> String {
        var param = "";
        if let path = NSBundle.mainBundle().pathForResource("config", ofType: "plist") {
            if let dict = NSDictionary(contentsOfFile: path) {
                if let val = dict.objectForKey(property) as? String {
                    param = val
                }
            }
        }
        return param
    }
    
    
}
