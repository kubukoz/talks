let pkgs = import
  (builtins.fetchTarball {
    url = "https://github.com/NixOS/nixpkgs/archive/20eabe33864104ad9928450e5927ab7835f8f9e8.tar.gz";
    sha256 = "0l00xjjd436ddq1llfvpcipz06b6s0zxqrmfm9mgj7c0ig4y4c0r";
  })
  { }; in

pkgs.mkShell {
  packages = [ pkgs.sbt pkgs.coursier pkgs.nodejs ];

  JAVA_OPTS = "-Xmx4G";
}
