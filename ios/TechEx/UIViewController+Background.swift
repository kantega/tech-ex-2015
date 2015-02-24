//
//  UIView+Background.swift
//  TechEx
//
//  Created by Kristian Lier Selnæs on 23/02/15.
//  Copyright (c) 2015 Technoport. All rights reserved.
//

import Foundation
import UIKit

extension UIViewController {
    
    func setTechExBackgroundImage() {
        self.view.backgroundColor = UIColor(patternImage: UIImage(named: "Background")!)
    }
    
}