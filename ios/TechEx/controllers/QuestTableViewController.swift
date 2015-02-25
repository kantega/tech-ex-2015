//
//  QuestTableViewController.swift
//  TechEx
//
//  Created by Kristian Lier Selnæs on 19/02/15.
//  Copyright (c) 2015 Technoport. All rights reserved.
//

import UIKit

class QuestTableViewController: UITableViewController {

    let badgeCellIdentifier = "BadgePrototypeCell"
    var quest = Quest()
    
    override func viewDidLoad() {
        self.navigationItem.titleView = UIImageView(image: UIImage(named: "NavbarLogo"))
        self.tableView.backgroundColor = UIColor(patternImage: UIImage(named: "Background")!)
        // Hide empty table rows
        self.tableView.tableFooterView = UIView(frame: CGRectZero)
        
        self.tableView.estimatedRowHeight = 100
        self.tableView.rowHeight = UITableViewAutomaticDimension
        
        super.viewDidLoad()
    }

    override func viewDidAppear(animated: Bool) {
        NSNotificationCenter.defaultCenter().addObserver(self, selector: "showAwardView:", name: "badgeReceived", object: nil)
    }
    
    override func viewDidDisappear(animated: Bool) {
        NSNotificationCenter.defaultCenter().removeObserver(self)
    }
    
    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }
    
    func reloadView () {
        self.tableView!.reloadData()
    }

    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return quest.achievements.count
    }

   
    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCellWithIdentifier(badgeCellIdentifier, forIndexPath: indexPath) as BadgeTableViewCell
        return configureTableCell(cell, indexPath: indexPath)
    }

    
    func configureTableCell(cell: BadgeTableViewCell, indexPath: NSIndexPath) -> BadgeTableViewCell {
        let achievement = quest.achievements[indexPath.row]
        
        configureLabel(cell.titleLabel, text: achievement.title)
        configureLabel(cell.descriptionLabel, text: achievement.desc)

        cell.backgroundColor = UIColor.clearColor()
        var badge = achievement.achieved ? "BadgeCompleted": "BadgeUncompleted"
        cell.badgeImage.image = UIImage(named: badge);
        return cell
    }

    
    func configureLabel(label: UILabel?, text: String) {
        label?.text = text
        label?.textColor = UIColor.whiteColor()
        label?.numberOfLines = 0;
        label?.lineBreakMode = NSLineBreakMode.ByWordWrapping
    }
 
}
