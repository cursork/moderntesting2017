⌘⇥ slides mean to switch to `src/mt/generative.clj`. First ⌘⇥ is until line 30
in src/mt/generative.clj. Second is the rest of the file.

To get the REPL, install [boot](http://boot-clj.com) and call `boot repl`:

```
boot.user=> (require 'mt.generative :reload)
boot.user=> (in-ns 'mt.generative)
mt.generative=> (test-set-is-a-set) ;; etc...
```
