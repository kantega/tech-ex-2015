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
        super.viewDidLoad()
    }

    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }

    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return quest.achievements.count
    }

   
    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCellWithIdentifier(badgeCellIdentifier, forIndexPath: indexPath) as UITableViewCell
        return configureTableCell(cell, indexPath: indexPath)
    }

    
    func configureTableCell(cell: UITableViewCell, indexPath: NSIndexPath) -> UITableViewCell {
        let achievement = quest.achievements[indexPath.row]
        cell.textLabel?.text = achievement.title
        cell.textLabel?.textColor = UIColor.whiteColor()
        cell.detailTextLabel?.text = achievement.desc
        cell.detailTextLabel?.textColor = UIColor.whiteColor()
        cell.backgroundColor = UIColor.clearColor()
        return cell
    }

    /*
    // Override to support conditional editing of the table view.
    override func tableView(tableView: UITableView, canEditRowAtIndexPath indexPath: NSIndexPath) -> Bool {
        // Return NO if you do not want the specified item to be editable.
        return true
    }
    */

    /*
    // Override to support editing the table view.
    override func tableView(tableView: UITableView, commitEditingStyle editingStyle: UITableViewCellEditingStyle, forRowAtIndexPath indexPath: NSIndexPath) {
        if editingStyle == .Delete {
            // Delete the row from the data source
            tableView.deleteRowsAtIndexPaths([indexPath], withRowAnimation: .Fade)
        } else if editingStyle == .Insert {
            // Create a new instance of the appropriate class, insert it into the array, and add a new row to the table view
        }    
    }
    */

    /*
    // Override to support rearranging the table view.
    override func tableView(tableView: UITableView, moveRowAtIndexPath fromIndexPath: NSIndexPath, toIndexPath: NSIndexPath) {

    }
    */

    /*
    // Override to support conditional rearranging of the table view.
    override func tableView(tableView: UITableView, canMoveRowAtIndexPath indexPath: NSIndexPath) -> Bool {
        // Return NO if you do not want the item to be re-orderable.
        return true
    }
    */

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        // Get the new view controller using [segue destinationViewController].
        // Pass the selected object to the new view controller.
    }
    */

}
