//
//  UIViewController+AwardView.swift
//  TechEx
//
//  Created by Kristian Lier Selnæs on 24/02/15.
//  Copyright (c) 2015 Technoport. All rights reserved.
//

import Foundation
import UIKit

extension UIViewController {

    func showAwardView() {
        let storyboard = UIStoryboard(name: "Main", bundle: nil);
        let awardVC = storyboard.instantiateViewControllerWithIdentifier("awardViewController") as BadgeAwardViewController
        awardVC.modalPresentationStyle = UIModalPresentationStyle.FullScreen
        self.presentViewController(awardVC, animated: true, completion: nil)
    }
}