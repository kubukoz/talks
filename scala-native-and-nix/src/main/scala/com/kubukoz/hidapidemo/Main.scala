package com.kubukoz.hidapidemo

@main def demo = {
  println("connected devices: ")
  HID.instance.getInfos().foreach(println)
}
