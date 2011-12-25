GET /inebriator/reset
    Resets all the solenoids to the closed state.
    
GET /inebriator/pour/cocktail/{cocktail}
    Pours {cocktail}.
    
GET /inebriator/pour/straight/{ingredient}?count={count}
    Pours {count} units of {ingredient}.

POST /inebriator/cocktail/rum_and_coke
    Adds {cocktail} to the DB. The definition must be specified in the HTTP request, in JSON format.

GET /inebriator/cocktail/rum_and_coke
    Returns the definition of {cocktail} in the HTTP response, in JSON format.

DELETE /inebriator/cocktail/{cocktail}
    Deletes the cocktail named {cocktail} from the DB.

GET /inebriator/cocktails
    Returns the list of cocktails in the HTTP response, in JSON format.

GET /inebriator/ingredients
	Returns the list of ingredients in the HTTP response, in JSON format.