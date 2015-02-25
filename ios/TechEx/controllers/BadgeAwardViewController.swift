//
//  BadgeAwardViewController.swift
//  TechEx
//
//  Created by Kristian Lier Selnæs on 24/02/15.
//  Copyright (c) 2015 Technoport. All rights reserved.
//

import UIKit

class BadgeAwardViewController: UIViewController {

    @IBOutlet weak var awardText: UILabel!
    @IBOutlet weak var okButton: UIButton!
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.setTechExBackgroundImage()

    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    

}
