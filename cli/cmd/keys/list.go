// cmd/keys/list.go
package keys

import (
	"fmt"
	"os"

	"github.com/spf13/cobra"
	"github.com/Riviya/Keypilot/cli/client"
)

func newListCmd(gatewayURL *string) *cobra.Command {
	return &cobra.Command{
		Use:   "list",
		Short: "List all API keys stored in the gateway",
		RunE: func(cmd *cobra.Command, args []string) error {
			c := client.NewGatewayClient(*gatewayURL)
			keys, err := c.ListKeys()
			if err != nil {
				fmt.Fprintf(os.Stderr, "Error: %v\n", err)
				os.Exit(1)
			}

			if len(keys) == 0 {
				fmt.Println("No API keys found.")
				return nil
			}

			fmt.Printf("%-38s  %-12s  %-6s\n", "ID", "PROVIDER", "ACTIVE")
			fmt.Println("--------------------------------------------------------------")
			for _, k := range keys {
				fmt.Printf("%-38s  %-12s  %-6v\n", k.ID, k.Provider, k.Active)
			}

			return nil
		},
	}
}