//
//  BadgeListView.swift
//  TechEx
//
//  Created by Kristian Lier Selnæs on 23/02/15.
//  Copyright (c) 2015 Technoport. All rights reserved.
//

import UIKit

class BadgeListView: UIView {

    var totalBadges: Int = 0 {
        didSet {
            addEmptyBadges()
        }
    }
    
    var achievedBadges: Int = 0 {
        didSet {
            updateAchievedBadges()
        }
    };
    
    func addEmptyBadges() {
        removeSubviews()
        self.backgroundColor = UIColor.clearColor()
        
        for i in 1 ... totalBadges {
            let badgeImage = UIImageView(image: UIImage(named: "BadgeUncompletedSmall"))
            self.addSubview(badgeImage)
        }
    }
    
    func updateAchievedBadges() {
        NSLog("Updating achieved badges: \(achievedBadges):\(totalBadges)")
        for i in 0 ..< achievedBadges {
            let badgeView = self.subviews[i] as UIImageView
            badgeView.image = UIImage(named: "BadgeCompletedSmall")
        }
    }

    func removeSubviews() {
        for v in self.subviews {
            v.removeFromSuperview()
        }
    }

    
    
    override func layoutSubviews() {
        for i in 0 ..< totalBadges {
            let badgeView = self.subviews[i] as UIImageView
            badgeView.frame.origin.x = CGFloat(i) * badgeView.frame.size.width + CGFloat(i)*5
            badgeView.frame.origin.y = (self.frame.size.height - badgeView.frame.size.height)/2
        }
    }

}
