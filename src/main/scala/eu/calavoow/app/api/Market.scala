package eu.calavoow.app.api

import eu.calavoow.app.api.Models.{ItemTypes, Root}
import org.slf4j.LoggerFactory

import scala.collection.mutable

object Market {
	val logger = LoggerFactory.getLogger(getClass)

	def getRegions(auth: String) = {
		val oAuth = Some(auth)
		val root = Root.fetch(oAuth)
		val regions = root.regions.followLink(oAuth)

		regions.items
	}

	def getMarketOrders(regionName: String, itemTypeName: String, auth: String) = {
		val oAuth = Some(auth)
		val root = Root.fetch(oAuth)
		val regions = root.regions.followLink(oAuth)
		logger.debug(regions.toString)
		val region = regions.items.find(_.name == regionName).map(_.href.followLink(auth))
		logger.debug(region.toString)

		// Get the itemType url.
		val itemTypes = getAllItemTypes(auth)
		val itemTypeLink = itemTypes.find(_.name == itemTypeName).map(_.href.href)

		for(
			regionInst ← region;
			itemLink ← itemTypeLink
		) yield {
			regionInst.marketBuyOrders.followLink(oAuth, Map("type" → itemLink))
		}

	}

	def getAllItemTypes(auth: String) : List[ItemTypes.Item] = {
		val oAuth = Some(auth)
		val root = Root.fetch(oAuth)
		val itemTypesRoot = root.itemTypes.followLink(oAuth)

		// Iterate trough all itemTypes pages and add them to the itemTypes list.
		val itemTypes = mutable.MutableList[ItemTypes.Item]()
		itemTypes ++= itemTypesRoot.items
		var currentItemTypes = itemTypesRoot
		while(currentItemTypes.next.isDefined) {
			currentItemTypes = currentItemTypes.next.get.followLink(oAuth)
			itemTypes ++= currentItemTypes.items
		}
		itemTypes.toList // Return the immutable list.
	}
}
