package com.example.app

import cats.effect.IOApp
import cats.effect.IO
import java.nio.file.Path

object Main extends IOApp.Simple:
  def run: IO[Unit] = IO.println("Hello Nix!")

case class Path(s: String)

trait HasPath {
  def path: Path
}

trait Derivation extends HasPath {
  def build: IO[Unit]
}

def derivation(
  name: String,
  buildDependencies: List[HasPath],
  src: Path,
  buildScript: Path => String,
  environment: Map[String, String] = Map.empty,
): Derivation = ???

class Nixpkgs {
  val coreutils: Derivation = ???
}

def use(path: String): Nixpkgs = ???

object demo {

  val pkgs = use("github:nixos/nixpkgs#7bd96c43cf54088e660967940f6d02056e0c0c8e")

  val simpleNixBuild = derivation(
    name = "example-0.0.1",
    buildDependencies = List(
      pkgs.coreutils
    ),
    src = Path("./."),
    buildScript = { outPath =>
      s"""
        cat a.txt b.txt > $outPath
      """
    },
  )

}
