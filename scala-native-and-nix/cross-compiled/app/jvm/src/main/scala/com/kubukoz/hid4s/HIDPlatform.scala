package com.kubukoz.hid4s

import scala.util.Using
import org.hid4java.HidManager
import scala.jdk.CollectionConverters.*
import org.hid4java.HidServices

trait HIDPlatform {

  val instance: HID = new {

    def getInfos(): List[DeviceInfo] =
      withHid {
        _.getAttachedHidDevices()
          .asScala
          .map { device =>
            DeviceInfo(device.getVendorId(), device.getProductId(), device.getProduct())
          }
          .toList
      }

    private def withHid[A](f: HidServices => A): A = Using.resource(HidManager.getHidServices())(f)(
      using _.shutdown()
    )

  }
//   def instance[F[_]: Sync: Console]: Resource[F, HID[F]] = Resource
//     .make(Sync[F].delay {
//       HidManager.getHidServices()
//     })(services => Sync[F].delay(services.shutdown()))
//     .map { services =>
//       new HID[F] {

//         def getDevice(vendorId: Int, productId: Int): Resource[F, Device[F]] = {
//           val findDevice = Sync[F]
//             .delay(Option(services.getHidDevice(vendorId, productId, null)))
//             .flatMap(_.liftTo[F](new Throwable("Device not found")))

//           Resource
//             .make(findDevice)(device => Sync[F].delay(device.close()))
//             .map(DevicePlatform.fromRaw)
//         }
//       }
//     }

}

// object DevicePlatform {

//   def fromRaw[F[_]: Sync](device: HidDevice): Device[F] = new Device[F] {

//     def read(bufferSize: Int): fs2.Stream[F, BitVector] = fs2
//       .Stream
//       .eval {
//         Sync[F].delay {
//           Array.fill[Byte](bufferSize)(0)
//         }
//       }
//       .flatMap { buffer =>
//         val loadBuffer = Sync[F].blocking(device.read(buffer))
//         val readBuffer = Sync[F].delay(BitVector(buffer))

//         fs2.Stream.repeatEval(loadBuffer *> readBuffer)
//       }

//   }

// }
