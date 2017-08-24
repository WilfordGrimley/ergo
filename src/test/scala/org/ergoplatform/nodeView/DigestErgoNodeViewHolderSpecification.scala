package org.ergoplatform.nodeView

import java.io.File

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import io.circe
import org.ergoplatform.modifiers.ErgoFullBlock
import org.ergoplatform.modifiers.history.Header
import org.ergoplatform.modifiers.mempool.AnyoneCanSpendTransaction
import org.ergoplatform.modifiers.mempool.proposition.{AnyoneCanSpendNoncedBox, AnyoneCanSpendProposition}
import org.ergoplatform.nodeView.history.ErgoHistory
import org.ergoplatform.nodeView.mempool.ErgoMemPool
import org.ergoplatform.nodeView.state.{DigestState, ErgoState}
import org.ergoplatform.nodeView.wallet.ErgoWallet
import org.ergoplatform.settings.ErgoSettings
import org.ergoplatform.utils.ErgoGenerators
import org.scalatest.{BeforeAndAfterAll, Matchers, PropSpecLike}
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import scorex.core.LocalInterface.LocallyGeneratedModifier
import scorex.core.NodeViewHolder.GetDataFromCurrentView
import scorex.testkit.TestkitHelpers

import scala.reflect.io.Path
import scala.util.Random

class DigestErgoNodeViewHolderSpecification extends
  TestKit(ActorSystem("DigestErgoNodeViewHolderSpec"))
  with ImplicitSender
  with PropSpecLike
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with ErgoGenerators
  with TestkitHelpers
  with BeforeAndAfterAll {

  lazy val settings: ErgoSettings = new ErgoSettings {
    override lazy val dataDir: String = s"/tmp/ergo/${Random.nextInt}"
    override def settingsJSON: Map[String, circe.Json] = Map()
  }

  override def beforeAll {
    new File(settings.dataDir).mkdirs()
  }

  override def afterAll {
    Path(new File(settings.dataDir)).deleteRecursively()
    TestKit.shutdownActorSystem(system)
  }

  property("genesis - state digest") {
    val digestHolder = system.actorOf(Props(classOf[DigestErgoNodeViewHolder], settings))

    digestHolder ! GetDataFromCurrentView[ErgoHistory, DigestState, ErgoWallet, ErgoMemPool, Boolean] { v =>
      v.state.rootHash.sameElements(ErgoState.afterGenesisStateDigest)
    }
    expectMsg(true)
  }

  property("genesis - history (no genesis block there yet)") {
    val digestHolder = system.actorOf(Props(classOf[DigestErgoNodeViewHolder], settings))

    digestHolder ! GetDataFromCurrentView[ErgoHistory, DigestState, ErgoWallet, ErgoMemPool, Option[Header]] { v =>
      v.history.bestHeaderOpt
    }
    expectMsg(None)
  }

  /*
  property("genesis - apply valid block") {
    val digestHolder = system.actorOf(Props(classOf[DigestErgoNodeViewHolder], settings))

    val (us, bh) = ErgoState.generateGenesisUtxoState(new File(s"/tmp/ergo/${Random.nextInt()}").ensuring(_.mkdirs()))

    val block = validFullBlock(None, us, bh)

    digestHolder ! LocallyGeneratedModifier[AnyoneCanSpendProposition.type, AnyoneCanSpendTransaction, ErgoFullBlock](block)

    digestHolder ! GetDataFromCurrentView[ErgoHistory, DigestState, ErgoWallet, ErgoMemPool, Option[Header]] { v =>
      v.history.bestHeaderOpt
    }

    expectMsg(Some(block.header))
  }*/
}
