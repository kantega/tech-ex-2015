//
//  QuestTableViewCell.swift
//  TechEx
//
//  Created by Kristian Lier Selnæs on 12/02/15.
//  Copyright (c) 2015 Technoport. All rights reserved.
//

import UIKit

class QuestTableViewCell: UITableViewCell {

    
    @IBOutlet weak var titleLabel: UILabel!
    @IBOutlet weak var badgesView: BadgeListView!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

    
}
