package net.liftweb.example.comet

/*                                                *\
  (c) 2007 WorldWide Conferencing, LLC
  Distributed under an Apache License
  http://www.apache.org/licenses/LICENSE-2.0
\*                                                 */

import net.liftweb.http._
import javax.servlet.http.{HttpServlet, HttpServletRequest , HttpServletResponse, HttpSession}
import net.liftweb.example.model._

class WebServices (val request: RequestState,val httpRequest: HttpServletRequest) extends SimpleController {
  
  def all_users: XmlResponse = {
    XmlResponse(<all_users>{
      User.findAll.map{u => u.toXml}
    }</all_users>)
  }
  
  def add_user: XmlResponse = {
    var success = false
    for (firstname <- param("firstname");
         lastname <- param("lastname");
         email <- param("email")) {
      val u = new User
      u.firstName(firstname).lastName(lastname).email(email)
      param("textarea").map{v => u.textArea(v)}
      param("password").map{v => u.password(v)}
      success = u.save
    }
    
    XmlResponse(<add_user success={success.toString}/>)
  }
}
