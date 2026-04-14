// cmd/keys/delete.go
package keys

import (
	"fmt"
	"os"

	"github.com/spf13/cobra"
	"github.com/Riviya/Keypilot/cli/client"
)

func newDeleteCmd(gatewayURL *string) *cobra.Command {
	var id string

	cmd := &cobra.Command{
		Use:   "delete",
		Short: "Delete an API key from the gateway",
		Example: `  gateway-cli keys delete --id <uuid>`,
		RunE: func(cmd *cobra.Command, args []string) error {
			if id == "" {
				return fmt.Errorf("--id is required")
			}

			c := client.NewGatewayClient(*gatewayURL)
			err := c.DeleteKey(id)
			if err != nil {
				fmt.Fprintf(os.Stderr, "Error: %v\n", err)
				os.Exit(1)
			}

			fmt.Printf("🗑️  Key %s deleted successfully\n", id)
			return nil
		},
	}

	cmd.Flags().StringVar(&id, "id", "", "ID of the API key to delete")

	return cmd
}