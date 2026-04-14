// cmd/keys/keys.go
package keys

import (
	"github.com/spf13/cobra"
)

// NewKeysCmd returns the "keys" command group with all subcommands attached.
// gatewayURL is passed by pointer so it reflects the flag value at runtime.
func NewKeysCmd(gatewayURL *string) *cobra.Command {
	keysCmd := &cobra.Command{
		Use:   "keys",
		Short: "Manage API keys stored in the gateway",
	}

	keysCmd.AddCommand(newAddCmd(gatewayURL))
	keysCmd.AddCommand(newListCmd(gatewayURL))
	keysCmd.AddCommand(newDeleteCmd(gatewayURL))

	return keysCmd
}