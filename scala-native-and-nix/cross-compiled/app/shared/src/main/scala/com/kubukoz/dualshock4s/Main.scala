package com.kubukoz.hidapidemo

import com.kubukoz.hid4s.HID

@main def demo = {
  println("connected devices: ")
  HID.instance.getInfos().foreach(println)
}
