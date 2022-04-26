package com.example.app

import cats.effect.IOApp
import cats.effect.IO

object Main extends IOApp.Simple:
  def run: IO[Unit] = IO.println("Hello Nix!")

case class Path(s: String)

trait HasPath {
  def path: IO[Path]
}

trait Package extends HasPath

def makePackage(
  dependencies: List[Package],
  src: Path,
  buildScript: String,
): Package = ???

def derivation(
  name: String,
  dependencies: List[HasPath],
  src: Path,
  buildScript: String,
  environment: Map[String, String] = Map.empty,
): Package = ???

class Nixpkgs {
  val coreutils: Package = ???
}

def use(path: String): Nixpkgs = ???

object demo {

  val pkgs = use("github:nixos/nixpkgs#7bd96c43cf54088e660967940f6d02056e0c0c8e")

  val simpleNixBuild = derivation(
    name = "example-0.0.1",
    dependencies = List(
      pkgs.coreutils
    ),
    src = Path("./."),
    buildScript = """
      cat a.txt b.txt > $outPath
    """,
  )

  val result: IO[Path] = simpleNixBuild.path
}
