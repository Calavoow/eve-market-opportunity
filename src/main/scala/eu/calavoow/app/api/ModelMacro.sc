import spray.json._
val json = """{"href": "https://crest-tq.eveonline.com/regions/10000006/","name": "Wicked Creek"}"""
val jsonAST = json.parseJson
jsonAST.convertTo[Item]
