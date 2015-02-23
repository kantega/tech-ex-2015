//
//  BadgeTableViewCell.swift
//  TechEx
//
//  Created by Kristian Lier Selnæs on 20/02/15.
//  Copyright (c) 2015 Technoport. All rights reserved.
//

import UIKit

class BadgeTableViewCell: UITableViewCell {

    @IBOutlet weak var titleLabel: UILabel!
    @IBOutlet weak var descriptionLabel: UILabel!
    @IBOutlet weak var badgeImage: UIImageView!
     
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

}
