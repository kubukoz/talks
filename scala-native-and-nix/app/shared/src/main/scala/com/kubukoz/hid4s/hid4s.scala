package com.kubukoz.hid4s

trait HID {
  def getInfos(): List[DeviceInfo]
}

object HID extends HIDPlatform

case class DeviceInfo(vendorId: Int, productId: Int, productString: String)
