/*
 *  Copyright (c) 2011, Franz Bettag <franz@bett.ag>
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * All advertising materials mentioning features or use of this software
 *       must display the following acknowledgement:
 *       This product includes software developed by the Bettag Systems UG
 *       and its contributors.
 *
 *  THIS SOFTWARE IS PROVIDED BY BETTAG SYSTEMS UG ''AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL BETTAG SYSTEMS UG BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package code
package model

import net.liftweb.mapper._
import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.http.{S,SessionVar}
import net.liftweb.sitemap.{Menu}


object User extends User with MetaMegaProtoUser[User] {
	override def dbTableName = "users" // define the DB table name
	override def fieldOrder = List(id, email, locale, timezone, password, hashrate, shares_total, shares_round, shares_stale, shares_round_estimate, donatePercent, locked)
	override def signupFields = List(email, locale, timezone, password)

	override val basePath: List[String] = "users" :: Nil
	override def skipEmailValidation = true

	override def editUserMenuLoc: Box[Menu] = Empty
	override def validateUserMenuLoc: Box[Menu] = Empty
	//override def lostPasswordMenuLoc: Box[Menu] = Empty
	//override def resetPasswordMenuLoc: Box[Menu] = Empty

	object loginReferer extends SessionVar("/account")

	override def homePage = {
		var ret = loginReferer
		loginReferer.remove()
		ret
	}

	override def login = {
		for (r <- S.referer if loginReferer.is == "/account") loginReferer.set(r)
		super.login
	}

	def isAdmin_?(): Boolean = this.currentUser match {
		case Full(u: User) => (u.superUser)
		case _ => false
	}

	override def screenWrap = Full(
		<lift:surround with="smashing" at="contentbody">
				<lift:bind/>
		</lift:surround>
		)


	override def signupXhtml(user: TheUserType) = { 
		(<form method="post" action={S.uri}>
			<h2>{ S.??("sign.up") }</h2>
			<table>
				{localForm(user, false, signupFields)} 
				<tr><td>&nbsp;</td><td><user:submit/></td></tr> 
			</table>
		</form>)
	}
}

class User extends MegaProtoUser[User] {
	def getSingleton = User

	object locked extends MappedBoolean(this) {
		override def dbIndexed_? = true
		override def dbNotNull_? = true
		override def defaultValue = false
	}

	object wallet extends MappedString(this, 255)

	object hashrate extends MappedInt(this)
	
	object shares_total extends MappedInt(this)
	object shares_round extends MappedInt(this)
	object shares_stale extends MappedInt(this)
	object shares_round_estimate extends MappedInt(this)

	object payoutlock extends MappedBoolean(this) {
		override def dbNotNull_? = true
		override def defaultValue = false
	}

	object idlewarning extends MappedBoolean(this) {
		override def dbNotNull_? = true
		override def defaultValue = false
	}

	object donatePercent extends MappedDecimal(this, java.math.MathContext.DECIMAL64, 2) {
		override def dbNotNull_? = true
		override def defaultValue = 0
	}

	def balance = 40.02
	def balances: List[AccountBalance] = AccountBalance.findAll(By(AccountBalance.user, this.id))
	def workers: List[PoolWorker] = PoolWorker.findAll(By(PoolWorker.user, this.id),OrderBy(PoolWorker.username, Ascending))
	def shares: List[Share] = Share.findAll(By(Share.username, this.email))
	def shareHistory: List[ShareHistory] = ShareHistory.findAll(By(ShareHistory.user, this.id))
}
