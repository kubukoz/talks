let
  nixpkgs = builtins.fetchTarball {
    url = "https://github.com/nixos/nixpkgs/archive/fd9651770056cdbf969d55fef5cae8ff1d8ed51f.tar.gz";
    sha256 = "0pyg73midga3v8wwmimbr6n436d9fdwc0s64cn8934qk4x5ydwiv";
  };
  pkgs = import nixpkgs { };
in
pkgs.mkShell {
  packages = [ pkgs.sbt pkgs.coursier pkgs.nodejs ];
}
