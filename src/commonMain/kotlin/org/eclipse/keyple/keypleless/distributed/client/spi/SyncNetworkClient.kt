/* **************************************************************************************
 * Copyright (c) 2024 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.keypleless.distributed.client.spi

import kotlin.coroutines.cancellation.CancellationException
import org.eclipse.keyple.keypleless.distributed.client.protocol.MessageDTO

class ServerIOException(override val message: String) : Exception(message)

/**
 * The network transmission abstraction used to communicate with a Keyple server. You can use the
 * provided @see SimpleHttpNetworkClient for publicly accessible http keyple servers (it just
 * supports basic auth). If you have more constraints, for authentication for example, use this
 * interface and implement your own authentication logics.
 */
abstract class SyncNetworkClient {

  /**
   * Actual method to transmit the MessageDTO payload to the server, and retrieve back the next
   * MessageDTO the server asks us to process. You must throw a ServerIOException in case of errors.
   *
   * @throws ServerIOException
   * @throws CancellationException
   */
  @Throws(ServerIOException::class, CancellationException::class)
  abstract suspend fun sendRequest(message: MessageDTO): List<MessageDTO>
}
