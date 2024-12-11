package com.kubukoz.hidapidemo

import libhidapi.all.*

import scalanative.unsafe.*
import scalanative.unsigned.*
import scalanative.*
import com.kubukoz.hidapidemo.CExtras.wcstombs

trait HID {
  def getInfos(): List[DeviceInfo]
}

case class DeviceInfo(vendorId: Int, productId: Int, productString: String)

object HID {

  def instance: HID = new {

    def getInfos(): List[DeviceInfo] = {
      require(hid_init() == 0)

      try {
        val deviceInfoStart = hid_enumerate(0.toUShort, 0.toUShort)

        require(deviceInfoStart != null)

        try
          (deviceInfoStart :: List
            .unfold(deviceInfoStart) { deviceInfo =>
              Option((!deviceInfo).next).map(dev => (dev, dev))
            })
            .map { deviceInfo =>
              val vendorId = (!deviceInfo).vendor_id
              val productId = (!deviceInfo).product_id

              val productString = fromwchar_tstring((!deviceInfo).product_string)

              DeviceInfo(vendorId.toInt, productId.toInt, productString)
            }
        finally hid_free_enumeration(deviceInfoStart)
      } finally hid_exit()
    }

  }

}

private def fromwchar_tstring(input: Ptr[wchar_t]): String = {

  val inputPtr = stackalloc[Ptr[wchar_t]](1)
  !inputPtr = input

  val utf8Size = wcstombs(null, input, 0.toCSize)

  if utf8Size == -1 then throw new RuntimeException("wcsrtombs failed")

  val utf8Bytes = new Array[Byte](utf8Size.toInt)

  val written = wcstombs(utf8Bytes.atUnsafe(0), input, utf8Size)

  if written == -1 then throw new RuntimeException("wcsrtombs failed")

  new String(utf8Bytes)
}

@extern
private object CExtras {
  @extern
  def wcstombs(dest: Ptr[Byte], src: Ptr[wchar_t], n: CSize): CSize = extern
}
