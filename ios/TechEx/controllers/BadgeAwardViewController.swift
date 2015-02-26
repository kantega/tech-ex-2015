//
//  BadgeAwardViewController.swift
//  TechEx
//
//  Created by Kristian Lier Selnæs on 24/02/15.
//  Copyright (c) 2015 Technoport. All rights reserved.
//

import UIKit

class BadgeAwardViewController: UIViewController {

    @IBOutlet weak var congratulationsLabel: UILabel!
    @IBOutlet weak var awardLabel: UILabel!
    @IBOutlet weak var okButton: UIButton!
    //Set in UIViewController+AwardView
    var badgeText = "You have been awarded the ... badge"
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        congratulationsLabel.textColor = UIColor(red: 152/255, green: 204/255, blue: 204/255, alpha: 1)
        awardLabel.textColor = UIColor.whiteColor()
        awardLabel.text = badgeText
        okButton.tintColor = UIColor.whiteColor()
        self.setTechExBackgroundImage()
    }



}
