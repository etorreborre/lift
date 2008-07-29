/*
 * Copyright 2008 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

package net.liftweb.openid

import net.liftweb._
import mapper._
import http._
import js._
import JsCmds._
import sitemap.{Loc, Menu}
import util.{Can, Empty, Full, Helpers}
import Helpers._

import org.openid4java.consumer._
import org.openid4java.discovery.Identifier;


trait MetaOpenIDProtoUser[ModelType <: OpenIDProtoUser[ModelType], MyType <: ModelType with MetaMegaProtoUser[ModelType, MyType]] extends MetaMegaProtoUser[ModelType, MyType] {
  self: MyType =>
  
  def superUser_? : Boolean = currentUser.map(_.superUser.is).openOr(false)
  
  override def signupFields: List[BaseOwnedMappedField[ModelType]] = nickname ::
  firstName :: lastName :: locale :: timezone :: Nil
  
  override def fieldOrder: List[BaseOwnedMappedField[ModelType]] = nickname ::
  firstName :: lastName :: locale :: timezone :: Nil
  
  def findOrCreate(openId: String): ModelType =
  find(By(this.openId, openId)) match {
    case Full(u) => u
    case _ =>
      self.create.openId(openId).
      nickname("change"+Helpers.randomInt(1000000000)).firstName("Unknown").
      lastName("Unknown").password(Helpers.randomString(15)).
      email(Helpers.randomInt(100000000)+"unknown@unknown.com").
      saveMe
  }
  
  // no need for these menu items with OpenID
  /**
   * The menu item for creating the user/sign up (make this "Empty" to disable)
   */
  override def createUserMenuLoc: Can[Menu] = Empty
  
  /**
   * The menu item for lost password (make this "Empty" to disable)
   */
  override def lostPasswordMenuLoc: Can[Menu] = Empty
  
  /**
   * The menu item for resetting the password (make this "Empty" to disable)
   */
  override def resetPasswordMenuLoc: Can[Menu] = Empty
  
  /**
   * The menu item for changing password (make this "Empty" to disable)
   */
  override def changePasswordMenuLoc: Can[Menu] = Empty
  
  /**
   * The menu item for validating a user (make this "Empty" to disable)
   */
  override def validateUserMenuLoc: Can[Menu] = Empty

  override def loginXhtml =
  <form method="POST" action={S.uri}>
    <table>
      <tr>
        <td colspan="2">{S.??("log.in")}</td>
      </tr>
      <tr>
        <td>OpenID</td><td><user:openid /></td>
      </tr>
      <tr>
        <td>&nbsp;</td><td><user:submit /></td>
      </tr>
    </table>
  </form>
  
  def openIDVendor: OpenIdVendor
  
  override def login = {
    if (S.post_?) {
      S.param("username").
      foreach(username => 
              openIDVendor.loginAndRedirect(username, performLogUserIn)
      )
    }
    
    def performLogUserIn(openid: Can[Identifier], fo: Can[VerificationResult], exp: Can[Exception]): ResponseIt = {
      (openid, exp) match {
        case (Full(id), _) =>
          val user = self.findOrCreate(id.getIdentifier)
          logUserIn(user)
          S.notice(S.??("Welcome ")+user.niceName)

        case (_, Full(exp)) =>
          S.error("Got an exception: "+exp.getMessage)
            

        case _ =>
          S.error("Unable to log you in: "+fo)
      }

      RedirectResponse("/")
    }
    
    Helpers.bind("user", loginXhtml,
                 "openid" -> (FocusOnLoad(<input type="text" name="username"/>)),
                 "submit" -> (<input type="submit" value={S.??("log.in")}/>))
  }

  private[openid] def findByNickname(str: String): List[ModelType] =
  findAll(By(nickname, str))
}

/**
 * An OpenID friendly extension to ProtoUser
 */
trait OpenIDProtoUser[T <: OpenIDProtoUser[T]] extends MegaProtoUser[T] {
  self: T =>
  
  def primaryKeyField = id
  
  override def getSingleton: KeyedMetaMapper[Long, T] with MetaOpenIDProtoUser[OpenIDProtoUser[T], T]
  
  object openId extends MappedString(this, 512) {
    override def dbIndexed_? = true
  }
  
  object nickname extends MappedPoliteString(this, 64) {
    override def dbIndexed_? = true
    
    override def setFilter = notNull _ :: toLower _ :: trim _ :: super.setFilter
    
    private def validateNickname(str: String): List[FieldError] = {
      val others = getSingleton.findByNickname(str).
      // getSingleton.findAll(By(getSingleton.nickname, str)).
      filter(_.id.is != fieldOwner.id.is)
      others.map(u => FieldError(this, <xml:group>Duplicate nickname: {str}</xml:group>))
    }
    
    override def validations = validateNickname _ :: super.validations
  }
  
  override def niceName: String = nickname
}
