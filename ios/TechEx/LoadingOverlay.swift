//
//  LoadingOverlay.swift
//  TechEx
//
//  Created by Kristian Lier Selnæs on 28/01/15.
//  Copyright (c) 2015 Technoport. All rights reserved.
//

import UIKit

class LoadingOverlay {
    
        var overlayView = UIView()
        var activityIndicator = UIActivityIndicatorView()
        
        class var shared: LoadingOverlay {
            struct Static {
                static let instance: LoadingOverlay = LoadingOverlay()
            }
            return Static.instance
        }
        
        func showOverlay(parent: UIView) {
            
            overlayView.frame = CGRectMake(0, 0, 80, 80)
            overlayView.center = parent.center
            overlayView.backgroundColor = UIColor.blackColor()
            overlayView.alpha = 0.5
            overlayView.clipsToBounds = true
            overlayView.layer.cornerRadius = 10
            
            activityIndicator.frame = CGRectMake(0, 0, 40, 40)
            activityIndicator.activityIndicatorViewStyle = .WhiteLarge
            activityIndicator.center = CGPointMake(overlayView.bounds.width / 2, overlayView.bounds.height / 2)
            
            overlayView.addSubview(activityIndicator)
            parent.addSubview(overlayView)
            
            activityIndicator.startAnimating()
        }
        
        func hideOverlayView() {
            activityIndicator.stopAnimating()
            overlayView.removeFromSuperview()
        }
   
}
